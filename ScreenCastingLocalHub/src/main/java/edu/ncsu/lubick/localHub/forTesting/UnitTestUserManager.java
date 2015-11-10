package edu.ncsu.lubick.localHub.forTesting;

import java.io.File;

import edu.ncsu.lubick.localHub.UserManager;

public class UnitTestUserManager extends UserManager {

	private boolean deployedGUI;
	
	
	public UnitTestUserManager(File workingDir)
	{
		super(workingDir);
	}
	
	public UnitTestUserManager(String testName, String testEmail, String testToken)
	{
		setData(testName, testEmail, testToken); 
	}
	
	public boolean hadToDeployGUIPrompt()
	{
		return deployedGUI;		
	}

	@Override
	public void writeOutInitFile(File initFile)
	{
		super.writeOutInitFile(initFile);
	}
	
	@Override
	public void promptUserForInfo()
	{
		deployedGUI = true;
	}

	public final void setData(String testName, String testEmail, String testToken)
	{
		setName(testName);
		setEmail(testEmail);
		setToken(testToken);
		
	}

	public boolean needsUserInput()
	{
		return needsUserInfo;
	}

	public static UserManager quickAndDirtyUser()
	{
		return new UnitTestUserManager("Test User","test@mailinator.com","123");
	}

}
