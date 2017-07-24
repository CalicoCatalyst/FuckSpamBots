package net.insxnity.bottemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.UserContributionPaginator;

public class Main extends TimerTask {
	
	public static Bot bot = new Bot();

	public static void main(String args[]) throws NetworkException, OAuthException {
		// This needs to be run or we basically can't do shit
		bot = setupBot();

		Timer timer = new Timer();
		// Cycle every x seconds
		Integer seconds = 30;
		// Third arg is time in millis, so we take 1000 milli (1 second) times
		// How many seconds we want
		timer.schedule(new Main(), 0, 1000 * seconds);
	}
	
	@Override
	public void run() {
		// This will look through the file of flagged users and return ones with 
		// the proper arguments
		List<String> flaggedUsers = getFlaggedUsers();
		
		for (String i : flaggedUsers) {
			// Let me know when it starts a cycle on a user
			Util.log("scanning " + i);
			
			// Scan through the user and reply to his comments. 
			// Basically handles everything we need done for x user
			scanAndReply(i);
		}
	}
	
	public static Bot setupBot() {
		// Credentials made in user/preferences/apps
		Util.log("Generating Credentials");
		bot.setUsername("");
		bot.setPassword("");
		bot.setPublicKey("");
		bot.setPrivateKey("");

		// Make a Credentials object using the above stuff
		bot.createCredentials();
		
		// Make a User Agent. This can be customized, and it's standard to,
		// But I have omitted that because I'm lazy
		bot.createUserAgent();
		
		// bot.setupBot() configures most of the functions we need to use,
		// like all of the accountmanagers and shit. It returns the configured bot.
		// Will return null if credentials aren't provided
		return bot.setupBot();
	}
	
	public static void scanAndReply(String user) {
		// Makes a Paginator of all of the users comments
		UserContributionPaginator p = new UserContributionPaginator(bot.getRedditClient(), "comments", user);
		
		// I have no idea what this does because it's not documented, but it
		// gets an absolute shit ton of comments, and thats what I need, so yay
		List<Contribution> comments = p.accumulateMerged(100);
		
		// Load the parsed comment id's from file. this is generally huge, so we
		// Try and limit how much reading a file we do here
		Util.getParsedIDBlob();
		
		// Var for stopping parsing once I'm getting into comments I've
		// already parsed
		Integer j = 0;
		
		// Scan through his comments
		for (Contribution i : comments) {
			// This method is modified from a different bot I made. 
			// Basically it keeps checking the parent object of the comment until
			// That parent object is a submission.
			// Since we cant get the subreddit of a comment, we have to use this
			// And get that submission's subreddit
			String sub = scrambleFromComment((Comment) i).getSubredditName();
			
			// Check if it hasn't been parsed, and the sub isn't on the blacklist
			// Only works if both are false
			if (!Util.isParsed(i) && !Util.getSubredditBlackList().contains(sub)) {
				// Reply the generated text for the user
				Util.simpleReply(i, getCommentTextForUser(user));
				// Let me know it replied (So I can guess if it's going haywire
				// and get an idea of exactly how much it's commenting
				Util.log("replied to " + user);
				// Add the sub to the blacklist. I have to manually clear the list atm, 
				// Which is cool, because I dont want to spam subreddits
				Util.addSubToBlackList(sub);
				
				// Save it to the parsed file
				Util.addParsed(i);
			}
			if (Util.isParsed(i)) {
				// If we've found 5 already-parsed comments, stop looping through
				if (j==5) break;
				// Count how many already-parsed comments we've found
				if (j<5) j=j+1;
			}
		}
		Util.log("scan completed");
	}
	
	public static Submission scrambleFromComment(Comment c) {
		// Make it into a Contribution
		Contribution it = c;
		
		// FullName's of contributions contain a tx variable
		// t1 = Comment
		// t2 = Submission
		// We're going to get the parent contribution's type till its a 
		// Submission "t2"
		while (it.getFullName().contains("t1") ) {
			// The contribution we're checking is now the parent one's ID
			it = Util.getContributionByID(((Comment) it).getParentId());
		}
		
		// Return the submission once we've reached it
		return (Submission) it;
		
	}
	
	public static String getCommentTextForUser(String user) {
		
		// We'll use this because I'm not some sloppy asshole
		StringBuilder sb = new StringBuilder();
		
		// Get the level of spambot in the file
		String levelOfSpam = getFlagInfo(user).get("level");
		
		sb.append("This comment is here to let you and the mods know you've been flagged as a level " 
				+ levelOfSpam + " spambot.  ");
		
		// We need two newlines to get actual spaces
		sb.append("\n\n");
		
		sb.append("This bot breaks the following FuckSpamBots rules:\n\n");
		
		// Get The text all of the rules they've broken
		sb.append(getRuleSectionForUser(user));
		
		// Get the recommended text put in the flag file. Usually the same shit
		sb.append("It is suggested that you " + getFlagInfo(user).get("rectext"));
		
		sb.append("\n\n---\n\n");
		
		sb.append("This bot only comments once per sub per day.\n\n");
		
		sb.append("If you are a mod and would like to opt out, reply \"!fsboptout\" and distinguish your comment :)");
		
		
		// Return all of the shit we've just put together
		return sb.toString();
	}
	
	
	public static String getRuleSectionForUser(String user) {
		// Get the Hashmap of info provide in the flag file for the user
		HashMap<String, String> userInfo = getFlagInfo(user);
		StringBuilder sb = new StringBuilder();
		
		List<Integer> ruleVios = new ArrayList<Integer>();
		
		for (String i : userInfo.get("violations").split(",")) {
			// The rule violations in the file are split by commas
			// For each number, we'll add it to the list
			ruleVios.add(Integer.valueOf(i));
		}
		
		for (Integer i : ruleVios) {
			// Need a bullet for my rules
			sb.append("- ");
			
			// Grab the rule #'s text
			sb.append(getRuleText(i));
			
			// Two newlines for sexyness
			sb.append("\n\n");
		}
		
		return sb.toString();
	}
	
	public static String getRuleText(Integer ruleNumber) {
		
		// There's an easier way to do all of this shit but I'm too lazy
		// to think about it. 
		
		String one = "Do not harass specific users. Doing" 
				   + " so will result in you being reported to admins and flagged immediately";
		String two = "Any bot that attempts to incite drama will be flagged immidiately" 
				   + "";
		String three = "We don't need more YouTube and Wikipedia bots. Additionally, extremely spammy ones will be flagged"
				       + "";
		String four = "Avoid scanning /r/all for comments unless your bot is useful. "
				    + "Most spambots flagged are flagged because of this";
		String five = "If your bot is triggered by a specific phrase in a comment,"
				    + " the phrase must be longer than 10 characters and/or not a common phrase";
		String six = "If your bot serves no useful purpose, the phrase that triggers it should not be common phrases. "
				   + "Summoning the bot should not accidentally happen often";
		String seven = "If your bot replies to generic phrases not directly intened to summon your bot, "
				+ "It should offer a simple and preferably automatic way to opt out";
		String eight = "If your bot comments more than 10 times an hour and breaks any of the above rules "
				    + "It will automatically be flagged for spam";
		
		HashMap<Integer, String> rules = new HashMap<Integer, String>();
		
		rules.put(1, one);
		rules.put(2, two);
		rules.put(3, three);
		rules.put(4, four);
		rules.put(5, five);
		rules.put(6, six);
		rules.put(7, seven);
		rules.put(8, eight);
		
		return rules.get(ruleNumber);
	}
	
	public static List<String> getFlaggedUsers() {
		String fullFile = Util.readFile(BotFiles.userFlags);
		String[] userSections = fullFile.split(";");
		List<String> users = new ArrayList<String>();
		
		for (String i : userSections) {
			// Get the first value of each user section. Should be a username
			// I n                io 
			//       d e n t a t     n
			users.add(
					i
					.split(":")
					[0]
							);
		}
		return users;
	}
	
	/**
	 * Info should be put into the flag file like so:
	 * 
	 * SpamBotName:Rule#sViolated:LevelOfSpam:Reccomended Text
	 * ie.
	 * SpamBotName:2,4,6:3:Fuck Yourself
	 * 
	 * User Section: ;
	 * SubSections: :
	 * SubSubSections: ,
	 * 
	 * @param user
	 * @return
	 */
	public static HashMap<String, String> getFlagInfo(String user) {
		// Get the Fllags file
		String fullFile = Util.readFile(BotFiles.userFlags);
		
		// Split the file up into user sections
		String[] userSections = fullFile.split(";");
		HashMap<String, String> ret = new HashMap<String, String>();
		
		// Go through each user section
		for (String i : userSections) {
			String[] infoArray = i.split(":");
			if (
					infoArray[0]
					.toLowerCase()
					.contains(
							user
							.toLowerCase()
							)
					) {
				// name found
				// Make a hasmap of that shit
				ret.put("name", infoArray[0]);
				ret.put("violations", infoArray[1]);
				ret.put("level", infoArray[2]);
				ret.put("rectext", infoArray[3]);
				return ret;
			}
			
		}
		//name not found
		ret.put("name", "null");
		return ret;
		
	}
}
