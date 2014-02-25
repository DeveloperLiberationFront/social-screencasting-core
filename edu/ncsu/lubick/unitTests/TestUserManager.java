package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.Runner;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.util.FileUtilities;

public class TestUserManager {
	
	private static File workingDir = new File("Scratch/");
	

	@BeforeClass
	public static void setUpClass() throws Exception
	{
		try
		{
			URL url = Runner.class.getResource(LocalHub.LOGGING_FILE_PATH);
			PropertyConfigurator.configure(url);
			Logger.getRootLogger().info("Logging initialized");
		}
		catch (Exception e)
		{
			//load safe defaults
			BasicConfigurator.configure();
			Logger.getRootLogger().info("Could not load property file, loading defaults", e);
		}
		if (!workingDir.exists() && !workingDir.mkdir())
		{
			fail("Couldn't make scratch folder");
		}
	}
	
	@Before
	public void setUp() throws Exception
	{
		assertTrue(TestingUtils.clearOutDirectory(workingDir));
	}

	private void moveTestUserFileIntoPlace(String testUserFileName) throws IOException
	{
		File copiedFile = FileUtilities.copyFileToDir(TestingUtils.getTestUserFile(testUserFileName), workingDir);
		assertTrue(copiedFile.renameTo(new File(workingDir, UserManager.EXPECTED_USER_SETTINGS)));
	}

	@Test
	public void testValidUser() throws Exception
	{
		moveTestUserFileIntoPlace("existingUserFile.txt");
		
		UserManager um = new UnitTestUserManager(workingDir);
		assertEquals(um.getUserName(), "Kevin Lubick");
		assertEquals(um.getUserEmail(), "kjlubick@ncsu.edu");
		assertEquals(um.getUserToken(), "221ed3d8-6a09-4967-91b6-482783ec5313");
		assertFalse(((UnitTestUserManager) um).hadToDeployGUIPrompt());
	}
	
	@Test
	public void testNonCreatedUser() throws Exception
	{
		assertFalse(new File(workingDir,UserManager.EXPECTED_USER_SETTINGS).exists());
		
		UserManager um = new UnitTestUserManager(workingDir);
		
		assertTrue(((UnitTestUserManager) um).hadToDeployGUIPrompt());
	}

}
