package edu.ncsu.lubick.localHub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import javax.swing.JOptionPane;

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
		JSONObject json = new JSONObject();
		try {
			json.put("name", getUserName());
			json.put("email", getUserEmail());
			json.put("token", getUserToken());
		} catch (JSONException e) {
			logger.error("Problem serializing user info",e);
		}

		logger.info("Writing user info to file: " + initFile.getPath());
		try (FileWriter writer = new FileWriter(initFile);)
		{
			json.write(writer);
			writer.flush();
		} catch (JSONException | IOException e) {
			logger.error("Problem writing user info to file",e);
		}
	}

	public void promptUserForInfo()
	{
        String name = JOptionPane.showInputDialog(null, "What is your name?");
        setName(name);

        String email = JOptionPane.showInputDialog(null, "What is your email address?");
        setEmail(email);
        
        setToken(UUID.randomUUID().toString());
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
