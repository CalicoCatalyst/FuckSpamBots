/**
 * (#)Util.java 7/17/17 v1
 * 
 * This class is designed to compliment Insxnity's Bot Template. For most
 * functions to work, it requires the Bot class and the Main class. I would
 * not recommend using it outside of the template, as it'd be kinda useless. 
 * 
 * The code below is commented in a style that makes viewing comments from
 * the outside of the class much easier. I've also added '//'-style comments, 
 * just in case somebody needs to go through my source code
 * 
 * 
 * 
 * @author Insxnity
 */
package net.insxnity.bottemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dean.jraw.ApiException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

/**
 * The Utility Class; Contains many functions that have uses across different
 * bots. Requires a {@code bot} variable in the {@code Main} class for some
 * methods.
 * 
 * @author Insxnity
 * @see Bot
 *
 */
public class Util {
	public static Boolean debug = false;

	/**
	 * Holds all of the parsed comment id's, mashed together because
	 * fuck it it's faster
	 */
	public static String commented = "";

	/**
	 * Translates the contents of a text file into a string
	 * 
	 * @param file - The file that will be translated into String
	 * @return {@code String} containing file contents
	 */
	public static String readFile(File file) {
			  byte[] encoded = null;
			try {
				encoded = Files.readAllBytes(Paths.get(file.toURI()));
			} catch (IOException e) {
				log("Critical Error, file could not be read");
				log("Check if " + file.getName() + " exists");

				System.exit(1);
			}
			  return new String(encoded, Charset.defaultCharset());
	}
	/**
	 * Append a String onto the contents of a text file
	 * 
	 * @param file - The file to be modified
	 * @param txt - The text to be appended to the file
	 * @return {@code true}, if operation completes successfully 
	 */
	public static boolean appendToFile(File file, String txt) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(file, true);
			fw.append(txt);
			fw.close();
		} catch (IOException e) { 
			return false;
		}
		return true;
//5
	}
	
	/**
	 * One-Liner for logging/printing out system information
	 * 
	 * @param s - Text to log
	 */
	public static void log(String s) {
		System.out.println(s);
	}
	
	/**
	 * Reply to a Comment/Submission on Reddit.
	 * 
	 * @param c - The Comment/Submission to reply to
	 * @param text - The text of the reply
	 * @return {@code true}, if able to comment
	 */
	public static boolean simpleReply(Contribution c, String text) {
		try {
			if (debug) log("attempt replied");
			Main.bot.getAccountManager().reply(c, text);
		} catch (NetworkException | ApiException e) {
			if (e.getMessage().toLowerCase().contains("ratelimit"))
				log("Rate Limited");
			if (debug) log("failed");
			return false; //Unable to comment
			
		}
		return true;
	}

	
	/**
	 * Get 25 (Max) unparsed submissions from the subreddit. Will automatically
	 * remove already-parsed submissions
	 * 
	 * @param subreddit - Subreddit to grab submissions from
	 * @param numberFromTop - Number of submissions to get (=25)
	 * @return {@code List<Submission>} of submissions
	 */
	public static List<Submission> getUnparsedSubmissions(String subreddit, Integer numberFromTop) {
		SubredditPaginator sp = new SubredditPaginator(Main.bot.getRedditClient(), subreddit);
		Listing<Submission> submissions = sp.getCurrentListing();
		String parsed = getParsedIDBlob();
		for (Submission i : submissions) {
			if (parsed.toString().contains(i.getId())) submissions.remove(i);
		}
		return submissions;
	}
	
	public static String getSubredditBlackList() {
		return readFile(BotFiles.subBlackList);
	}
	public static void addSubToBlackList(String sub) {
		appendToFile(BotFiles.subBlackList, sub + "\n");
	}
	/**
	 * Extracts URL's from a string. 
	 * 
	 * @param text - String to extract URLs from
	 * @return {@code List<String>} of URLs
	 */
	public static List<String> extractUrls(String text)
	{
	    List<String> containedUrls = new ArrayList<String>();
	    String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
	    Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
	    Matcher urlMatcher = pattern.matcher(text);
	
	    while (urlMatcher.find())
	    {
	        containedUrls.add(text.substring(urlMatcher.start(0),
	                urlMatcher.end(0)));
	    }
	
	    return containedUrls;
	}
	/**
	 * Gets the amount of URL's in a String
	 * 
	 * @param text - String to scan for URLs
	 * @return {@code Integer} amount of URLs in {@code text}
	 */
	public static Integer urlCount(String text) {
		return extractUrls(text).size();
	}
	/**
	 * Gets a {@code Submission} from a {@code Comment} containing a link
	 * 
	 * @param c - Comment containing link
	 * @return Any {@code Submission} that could be created from found URLs
	 */
	public static Submission getSubmissionFromCommentLink(Comment c){
		String t = c.getBody();
		if (urlCount(t) <1) return null;
		String url = Util.extractUrls(t).get(0);
		return Main.bot.getRedditClient().getSubmission(getIdFromUrl(url));
	}
	public static String getIdFromUrl(String url){
		String id = url.split("/")[6];
		return id;
	}
	/**
	 * Gets all of the parsed contribution IDs in one, huge ass, spaceless
	 * blob, because speed
	 * 
	 * @return {@code String} containing Parsed IDs 
	 */
	public static String getParsedIDBlob() {
		commented = readFile(BotFiles.parsedComments);
		return commented;
	}
	/**
	 * Add a contribution's ID to the list of parsed ID's
	 * 
	 * @param c - Write a parsed ID to file
	 */
	public static void addParsed(Contribution c) {
		appendToFile(BotFiles.parsedComments, c.getId() + "\n");
		if (debug) log("added " + c.getId() + " to parsed comments");
	}
	public static boolean isParsed(Contribution c) {
		return commented.contains(c.getId());
	}
	
	public static Contribution getContributionByID(String id) {
		return (Contribution) (Main.bot.getRedditClient().get(id)).get(0);
	}

}
