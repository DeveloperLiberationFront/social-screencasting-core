package edu.ncsu.lubick.localHub;

import java.io.File;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.util.FileUtilities;

public class UserManager {

	public static final String EXPECTED_USER_SETTINGS = "user.ini";
	private String userName;
	private String userEmail;
	private String userToken;
	
	private static final Logger logger = Logger.getLogger(UserManager.class);

	
	protected UserManager()
	{
		
	}
	
	public UserManager(File initDirectory)
	{
		parseOutFile(new File(initDirectory, EXPECTED_USER_SETTINGS));
	}

	protected void setToken(String token)
	{
		this.userToken = token;
	}

	protected void setName(String userName)
	{
		this.userName = userName;
	}

	protected void setEmail(String userEmail)
	{
		this.userEmail = userEmail;
	}

	private void parseOutFile(File initFile)
	{
		if (!initFile.exists())
		{
			promptUserForInfo();
			writeOutInitFile(initFile);
			return;
		}
		String fileContents = FileUtilities.readAllFromFile(initFile);
		try
		{
			JSONObject parsedJObject = new JSONObject(fileContents);
			setName(parsedJObject.getString("name"));
			setEmail(parsedJObject.getString("email"));
			setToken(parsedJObject.getString("token"));
		}
		catch (JSONException e)
		{
			logger.info("Problem parsing User Init file",e);
		}
	}

	protected void writeOutInitFile(File initFile)
	{
		// TODO Auto-generated method stub
		
	}

	public void promptUserForInfo()
	{
		// TODO Auto-generated method stub
		
	}

	public String getUserName()
	{
		return userName;
	}

	public String getUserEmail()
	{
		return userEmail;
	}

	public String getUserToken()
	{
		return userToken;
	}

	public boolean needsUserInput()
	{
		return false;
	}



}
