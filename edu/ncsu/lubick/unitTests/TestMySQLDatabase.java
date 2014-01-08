package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.localHub.database.QueuedMySQLDatabase;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;

public class TestMySQLDatabase 
{
	private static Logger logger = Logger.getLogger(TestMySQLDatabase.class.getName());
	private static File testFile = new File(RemoteSQLDatabaseFactory.TEST_PATH_TO_USER_FILE);

	@BeforeClass
	public static void setUpClass() throws Exception
	{
		RemoteSQLDatabaseFactory.setTestingMode(true);

	}


	private QueuedMySQLDatabase db;

	@Before
	public void setUp() throws Exception
	{
		deleteTestFile();
		db = null;
	}
	
	@After
	public void tearDown() throws Exception
	{
		db.close();
	}

	private static void deleteTestFile()
	{
		if (testFile.exists() && !testFile.delete())
		{
			logger.error("Could not clear out testFile");
			fail("Could not clear out testFile");
		}
		assertFalse(testFile.exists());
	}

	@Test
	public void testDummyUser() throws Exception
	{		
		db = (QueuedMySQLDatabase) RemoteSQLDatabaseFactory.createMySQLDatabaseUsingUserFile();
		assertNotNull(db);
		assertTrue(db.isConnected());

		assertTrue(testFile.exists());
		UserNameIDStruct generatedInfo = extractNameEmailId(testFile);
		assertEquals("^DUMMY USER", generatedInfo.userName);
		assertEquals("^DUMMY_EMAIL", generatedInfo.email);
		assertNotNull(generatedInfo.id);
	}

	@Test
	public void testPartialUser() throws Exception
	{		
		UserNameIDStruct partialUser = new UserNameIDStruct();
		partialUser.userName = "^MY_TEST_USER";
		partialUser.email = "^MY_TEST_USER_EMAIL";
		writeOutUserFile(testFile,partialUser);
		assertTrue(testFile.exists());
		
		db = (QueuedMySQLDatabase) RemoteSQLDatabaseFactory.createMySQLDatabaseUsingUserFile();
		assertNotNull(db);
		assertTrue(db.isConnected());

		assertTrue(testFile.exists());
		UserNameIDStruct generatedInfo = extractNameEmailId(testFile);
		assertEquals("^MY_TEST_USER", generatedInfo.userName);
		assertEquals("^MY_TEST_USER_EMAIL", generatedInfo.email);
		assertNotNull(generatedInfo.id);
	}
	
	@Test
	public void testFullUserThatExists() throws Exception
	{		
		UserNameIDStruct user = new UserNameIDStruct();
		user.userName = "USER WHO EXISTS";
		user.email = "EMAIL_THAT_EXISTS";
		user.id = "82a4f159-ba6f-4874-8953-ff304f1a87fd";
		writeOutUserFile(testFile,user);
		assertTrue(testFile.exists());
		
		db = (QueuedMySQLDatabase) RemoteSQLDatabaseFactory.createMySQLDatabaseUsingUserFile();
		assertNotNull(db);
		assertTrue(db.isConnected());

		assertTrue(testFile.exists());
		UserNameIDStruct generatedInfo = extractNameEmailId(testFile);
		assertEquals("USER WHO EXISTS", generatedInfo.userName);
		assertEquals("EMAIL_THAT_EXISTS", generatedInfo.email);
		assertNotNull("82a4f159-ba6f-4874-8953-ff304f1a87fd");
	}


	private void writeOutUserFile(File file, UserNameIDStruct userStruct) throws Exception
	{
		try (FileOutputStream fos = new FileOutputStream(file);)
		{
			fos.write((userStruct.userName+"\n").getBytes());
			fos.write((userStruct.email+"\n").getBytes());
			if (userStruct.id != null)
			{
				fos.write((userStruct.id+"\n").getBytes());
			}
		}
		catch (Exception e) 
		{
			throw e;
		}
		
		
	}

	private static UserNameIDStruct extractNameEmailId(File file) throws Exception
	{
		UserNameIDStruct struct = new UserNameIDStruct();
		try(Scanner scanner = new Scanner(file);)
		{
			assertTrue(scanner.hasNext());

			struct.userName = scanner.nextLine();
			struct.email = scanner.nextLine();
			if (scanner.hasNext())
			{
				struct.id = scanner.nextLine();
			}
		}
		catch (Exception e) {
			throw e;
		}
		return struct;
	}


	private static class UserNameIDStruct
	{
		String userName, email, id;
	}

}
