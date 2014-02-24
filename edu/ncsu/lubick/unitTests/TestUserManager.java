package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.util.FileUtilities;

public class TestUserManager {
	
	private static File workingDir = new File("Scratch/");
	

	@BeforeClass
	public static void setUpClass() throws Exception
	{
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

	@Test
	public void testValidUser() throws Exception
	{
//		JSONObject jObject = new JSONObject();
//		jObject.put("name", "Kevin Lubick");
//		jObject.put("email", "kjlubick@ncsu.edu");
//		jObject.put("token", UUID.randomUUID().toString());
//		System.out.println(jObject.toString(2));
		moveTestUserFileIntoPlace("existingUserFile.txt");
		
		UserManager um = new UnitTestUserManager(workingDir);
		assertEquals(um.getUserName(), "Kevin Lubick");
		assertEquals(um.getUserEmail(), "kjlubick@ncsu.edu");
		assertEquals(um.getUserToken(), "221ed3d8-6a09-4967-91b6-482783ec5313");
		assertFalse(((UnitTestUserManager) um).hadToDeployGUIPrompt());
	}

	private void moveTestUserFileIntoPlace(String testUserFileName) throws IOException
	{
		File copiedFile = FileUtilities.copyFileToDir(TestingUtils.getTestUserFile(testUserFileName), workingDir);
		assertTrue(copiedFile.renameTo(new File(workingDir, UserManager.EXPECTED_USER_SETTINGS)));
	}

}
