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
	private String userId;
	
	private static final Logger logger = Logger.getLogger(UserManager.class);

	public UserManager(File initDirectory)
	{
		parseOutFile(new File(initDirectory, EXPECTED_USER_SETTINGS));
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
			this.userName = parsedJObject.getString("name");
			this.userEmail = parsedJObject.getString("email");
			this.userId = parsedJObject.getString("token");
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

	protected void promptUserForInfo()
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
		return userId;
	}



}
