package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.lubick.util.FileUtilities;

public class TestUserManager {
	
	private static File workingDir = new File("Scratch/");
	

	@BeforeClass
	public static void setUpClass() throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
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
		assertFalse(um.needsUserInput());
		assertEquals(um.getUserName(), "Kevin Lubick");
		assertEquals(um.getUserEmail(), "kjlubick@ncsu.edu");
		assertEquals(um.getUserToken(), "221ed3d8-6a09-4967-91b6-482783ec5313");
		assertFalse(((UnitTestUserManager) um).hadToDeployGUIPrompt());
	}
	
	@Test
	public void testNonCreatedUser() throws Exception
	{
		File expectedINIFile = new File(workingDir,UserManager.EXPECTED_USER_SETTINGS);
		assertFalse(expectedINIFile.exists());
		
		UnitTestUserManager testUm = new UnitTestUserManager(workingDir);
		assertTrue(testUm.needsUserInput());
		testUm.promptUserForInfo();

		assertTrue(testUm.hadToDeployGUIPrompt());
		
		testUm.setData("TestUser", "test@mailinator.com", "[SOME UUID]");
		testUm.writeOutInitFile(expectedINIFile);
		
		assertTrue(expectedINIFile.exists());
		
		String fileContents = FileUtilities.readAllFromFile(expectedINIFile);
		assertNotNull(fileContents);
		JSONObject jobj = new JSONObject(fileContents);
		assertTrue(jobj.has("name"));
		assertEquals(jobj.get("name"), "TestUser");
		assertTrue(jobj.has("email"));
		assertEquals(jobj.get("email"), "test@mailinator.com");
		assertTrue(jobj.has("token"));
		assertEquals(jobj.get("token"), "[SOME UUID]");
		
	}

}
