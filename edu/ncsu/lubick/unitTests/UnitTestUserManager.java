package edu.ncsu.lubick.unitTests;

import java.io.File;

import edu.ncsu.lubick.localHub.UserManager;

public class UnitTestUserManager extends UserManager {

	private boolean deployedGUI;
	
	
	public UnitTestUserManager(File workingDir)
	{
		super(workingDir);
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

	public void setData(String testName, String testEmail, String testUUID)
	{
		// TODO Auto-generated method stub
		
	}
}
