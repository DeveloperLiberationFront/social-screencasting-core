package edu.ncsu.lubick.unitTests;

import java.io.File;

import edu.ncsu.lubick.localHub.UserManager;

public class UnitTestUserManager extends UserManager {

	private boolean deployedGUI = false;
	
	
	public UnitTestUserManager(File workingDir)
	{
		super(workingDir);
	}
	
	public boolean hadToDeployGUIPrompt()
	{
		return deployedGUI;		
	}

}
