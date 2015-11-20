package edu.ncsu.dlf.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.dlf.localHub.UserManager;
import edu.ncsu.dlf.localHub.forTesting.TestingUtils;
import edu.ncsu.dlf.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.dlf.util.FileUtilities;

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
		assertFalse(((UnitTestUserManager) um).needsUserInput());
		assertEquals("Kevin Lubick", um.getUserName());
		assertEquals("kjlubick@ncsu.edu", um.getUserEmail());
		assertEquals("221ed3d8-6a09-4967-91b6-482783ec5313", um.getUserToken());
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
		assertEquals( "TestUser", jobj.get("name")); 
		assertTrue(jobj.has("email"));
		assertEquals("test@mailinator.com", jobj.get("email"));
		assertTrue(jobj.has("token"));
		assertEquals("[SOME UUID]", jobj.get("token"));
		
	}

}
