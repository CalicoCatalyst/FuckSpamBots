/**
 * (#)Bot.java 7/17/17 v1
 * 
 * This class is designed to streamline and concentrate many core functions of
 * the bot into one Object, which also eases the use of multiple accounts
 * for a single script. 
 * 
 * The code below is commented in a style that makes viewing comments from
 * the outside of the class much easier. I've also added '//'-style comments, 
 * just in case somebody needs to go through my source code
 * 
 * @author Insxnity
 * 
 */
package net.insxnity.bottemplate;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.managers.InboxManager;
import net.dean.jraw.managers.ModerationManager;

/**
 * Represents and performs most of the core logic functions
 * of the bot. Handles main, redundant interactions with JRAW
 * 
 * @author Insxnity
 */
public class Bot {
	
	private UserAgent botUserAgent;
	
	private Credentials botCredentials;

	private AccountManager accountManager;
	
	private InboxManager inboxManager;
	
	private ModerationManager moderationManager;
	
	private RedditClient redditClient;
	
	// User Agent Information
	
	private String botPlatform = 
			// You almost always want to use 'desktop' here
			""
	;
	private String botGroupId = 
			""
	;
	private String botVersion = 
			""
	;
	private String botAuthor = 
			""
	;
	
	// OAUTH Credential Information
	
	private String botUsername =
			""
	;
	private String botPassword = 
			""
	;
	private String botPublicKey = 
			""
	;
	private String botPrivateKey = 
			""
	;
	

	public Bot() {
		
	}
	
	/**
	 * Configures most of the internal bot variables. 
	 * UserAgent and Credentials must be configured before run
	 * 
	 * @return This bot object, or null, if UserAgent and 
	 *         Credentials have not been configured 
	 */
	public Bot setupBot() {
		if (botUsername.equals("") 
				|| botPassword.equals("") 
				|| botPublicKey.equals("") 
				|| botPrivateKey.equals("")) 
		{
			Util.log("Credentials Not Provided. Could not create credentials");
			return null;
		}
		
		this.generateClient();
		this.generateManagers();
		return this;
	}
	
	/**
	 * Configures the user agent for the bot using the variables
	 * already set, or a default set, if none have been configured
	 * 
	 * @see setPlatform()
	 * @see setGroupId()
	 * @see setVersion()
	 * @see setAuthor()
	 * @return The Generated UserAgent
	 */
	public UserAgent createUserAgent() {
		if (botPlatform.equals("")) botPlatform = "desktop";
		if (botGroupId.equals("")) botGroupId = "net.insxnity.template";
		if (botVersion.equals("")) botVersion = "0.1.0";
		if (botAuthor.equals("")) botAuthor = "Insxnity";
		
		setUserAgent(UserAgent.of(botPlatform, botGroupId, botVersion, botAuthor));
		return botUserAgent;
	}
	
	/**
	 * Configures the user agent for the bot with the parameters 
	 * provided
	 * 
	 * 
	 * @param platform - System Platform, usually "desktop"
	 * @param groupId - Unique ID for your application
	 * @param version - Version of your script
	 * @param author - Author of your script
	 * @return The generated UserAgent
	 */
	public UserAgent createUserAgent(String platform, String groupId, String version, String author) {
		if (botPlatform.equals("")) platform = "desktop";
		if (botGroupId.equals("")) groupId = "net.insxnity.template";
		if (botVersion.equals("")) version = "0.1.0";
		if (botAuthor.equals("")) author = "Insxnity";
		
		setUserAgent(UserAgent.of(platform, groupId, version, author));
		return botUserAgent;
	}
	/**
	 * Configures Credentials object for bot using provided credentials
	 * 
	 * @return The generated Credentials, or null, if the required credentials
	 *         are not avaliable
	 */
	public Credentials createCredentials() {
		if (botUsername.equals("") || botPassword.equals("") || botPublicKey.equals("") || botPrivateKey.equals("")) {
			Util.log("Credentials Not Provided. Could not create credentials");
			return null;
		}
		
		setCredentials(Credentials.script(botUsername, botPassword, botPublicKey, botPrivateKey));
		return botCredentials;
	}	
	
	/**
	 * Create Credentials using parameters provided
	 * 
	 * @param u - Username
	 * @param p - Password
	 * @param pu - Public Key
	 * @param pr - Private Key
	 * @return The generated Credentials, or null, if required credentials 
	 *         are not available
	 */
	public Credentials createCredentials(String u, String p, String pu, String pr) {
		
		if (u.equals("") || p.equals("") || pu.equals("") || pr.equals("")) {
			Util.log("Credentials Not Provided. Could not create credentials");
			return null;
		}
		
		setCredentials(Credentials.script(u, p, pu, pr));
		return botCredentials;
	}
	/**
	 * Generates and openes a reddit client using the generated UserAgent
	 * and Credentials
	 * 
	 * @return The generated RedditClient, or null, if one could not be 
	 *         generated
	 */
	public RedditClient generateClient(){

		setRedditClient(new RedditClient(this.getUserAgent()));

		OAuthData authData = null;
		try {
			authData = redditClient.getOAuthHelper().easyAuth(botCredentials);
		} catch (NetworkException | OAuthException e) {
			e.printStackTrace();
		}
		
		redditClient.authenticate(authData);
		
		
		
		return redditClient;
	}
	public Bot generateManagers() {
		this.setAccountManager(new AccountManager(this.getRedditClient()));
		this.setInboxManager(new InboxManager(this.getRedditClient()));
		this.setModerationManager(new ModerationManager(this.getRedditClient()));
		return this;
	}
	
	
	public static void everyHour() throws NetworkException, OAuthException {
		//OAuthData newAuthData = botRedditClient.getOAuthHelper().refreshToken(credentials);
		//botRedditClient.authenticate(newAuthData);
	}
	
	
	public void cleanup() {
		 //OAuthHelper.revokeToken(botCredentials);
	}
	
	
	
	
	
	
	// Start getters and setters

	/**
	 * @return The Bot's RedditClient variable
	 */
	public RedditClient getRedditClient() {
		return redditClient;
	}
	/**
	 * Set the RedditClient. This usually does not need to be done by the 
	 *         end user.
	 * 
	 * @param redditClient - Set the RedditClient to...
	 */
	private void setRedditClient(RedditClient redditClient) {
		this.redditClient = redditClient;
	}
	/** 
	 * @return the Bot's UserAgent variable
	 */
	public UserAgent getUserAgent() {
		return botUserAgent;
	}
	/**
	 * Set the UserAgent. This usually does not need to be done by the
	 *         end user.
	 * 
	 * @param botUserAgent - Set the RedditClient to...
	 */
	private void setUserAgent(UserAgent botUserAgent) {
		this.botUserAgent = botUserAgent;
	}
	/**
	 * Get the bot's credentials. 
	 * 
	 * @return Credentials variable for bot
	 */
	public Credentials getCredentials() {
		return botCredentials;
	}
	/**
	 * Set the credentials for the bot
	 * 
	 * @param botCredentials - Set the Credentials to...
	 */
	private void setCredentials(Credentials botCredentials) {
		this.botCredentials = botCredentials;
	}

	/**
	 * The AccountManager for the Bot. Used to manage most tasks
	 *         that tend to interact with Reddit.
	 * 
	 * @return The Bot's AccountManager variable
	 */
	public AccountManager getAccountManager() {
		return accountManager;
	}
	/**
	 * Set the AccountManager. The end user does not generally need to use
	 * this function.
	 * 
	 * @param accountManager - Set the AccountManager to...
	 */
	private void setAccountManager(AccountManager accountManager) {
		this.accountManager = accountManager;
	}
	
	/**
	 * The InboxManager for the Bot. Used to send mail and shit.
	 * 
	 * @return - The InboxManager for the Bot
	 */
	public InboxManager getInboxManager() {
		return inboxManager;
	}
	/**
	 * Set the InboxManager for the Bot. The end user does not generally need to 
	 *         use this task
	 * @param inboxManager - Set the InboxManager to...
	 */
	private void setInboxManager(InboxManager inboxManager) {
		this.inboxManager = inboxManager;
	}
	/**
	 * Get the ModerationManager for the bot. Assist with moderation duties 
	 *         like deleting, distinguishing, etc.
	 * 
	 * @return - The ModerationManager for the Bot
	 */
	public ModerationManager getModerationManager() {
		return moderationManager;
	}
	/**
	 * Set the ModerationManager for the Bot. The end user does not generally need to 
	 *         use this task
	 * @param moderationManager - Set the ModerationManager to...
	 */
	private void setModerationManager(ModerationManager moderationManager) {
		this.moderationManager = moderationManager;
	}

	public String getPlatform() {
		return botPlatform;
	}

	public void setPlatform(String botPlatform) {
		this.botPlatform = botPlatform;
	}

	public String getGroupId() {
		return botGroupId;
	}

	public void setGroupId(String botGroupId) {
		this.botGroupId = botGroupId;
	}

	public String getVersion() {
		return botVersion;
	}

	public void setVersion(String botVersion) {
		this.botVersion = botVersion;
	}

	public String getAuthor() {
		return botAuthor;
	}

	public void setAuthor(String botAuthor) {
		this.botAuthor = botAuthor;
	}

	public String getUsername() {
		return botUsername;
	}

	public void setUsername(String botUsername) {
		this.botUsername = botUsername;
	}

	public String getPassword() {
		return botPassword;
	}

	public void setPassword(String botPassword) {
		this.botPassword = botPassword;
	}

	public String getPublicKey() {
		return botPublicKey;
	}

	public void setPublicKey(String botPublicKey) {
		this.botPublicKey = botPublicKey;
	}

	public String getPrivateKey() {
		return botPrivateKey;
	}

	public void setPrivateKey(String botPrivateKey) {
		this.botPrivateKey = botPrivateKey;
	}

	
	
	
	
}
