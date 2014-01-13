package edu.ncsu.lubick.localHub.database;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public final class RemoteSQLDatabaseFactory
{

	private static final String PATH_TO_USER_FILE = "./username.txt";
	public static final String TEST_PATH_TO_USER_FILE = "./test_username.txt";

	private static boolean isUsingTestUserFile = false;
	private static boolean blockAllActualConnections = false;
	
	private static UserNameIDStruct userToCreate = null;

	private static File expectedUserNameAndFile = new File(PATH_TO_USER_FILE);

	public static void setUpToUseTestUserFile(boolean b)
	{
		isUsingTestUserFile = b;
		if (isUsingTestUserFile)
		{
			expectedUserNameAndFile = new File(TEST_PATH_TO_USER_FILE);
		}
		else
		{
			expectedUserNameAndFile = new File(PATH_TO_USER_FILE);
		}

	}
	
	public static void setUpToUseMockDB(boolean b)
	{
		blockAllActualConnections = b;
	}

	public static RemoteSQLDatabase createMySQLDatabaseUsingUserFile()
	{
		if (blockAllActualConnections)
		{
			return handleMockDB();
		}
		return handleNormalDB();
	}

	private static RemoteSQLDatabase handleMockDB()
	{
		return new MockRemoteSQLDatabase();
	}

	private static RemoteSQLDatabase handleNormalDB()
	{
		String userId;
		if (expectedUserNameAndFile.exists())
		{
			userId = readOrCreateNewUserId(expectedUserNameAndFile);
		}
		else
		{
			userId = createDummyUser();
		}
		QueuedMySQLDatabase newDB = new QueuedMySQLDatabase(userId);
		
		if (userToCreate != null)
		{
			newDB.registerNewUser(userToCreate.email, userToCreate.userName, userToCreate.id);
		}
		
		return newDB;
	}

	private static String createDummyUser()
	{
		UserNameIDStruct struct = new UserNameIDStruct();
		if (!isUsingTestUserFile)
		{
			struct.userName = "USER NAME";
			struct.email = "USER EMAIL";
		}
		else
		{
			struct.userName = "^DUMMY USER";
			struct.email = "^DUMMY_EMAIL";
		}
		struct.id = UUID.randomUUID().toString();

		try
		{
			writeOutUserFile(expectedUserNameAndFile, struct);
		}
		catch (IOException e)
		{
			throw new DBAbstractionException(e);
		}
		userToCreate = struct;
		return struct.id;

	}

	private static String readOrCreateNewUserId(File userNameFile)
	{
		UserNameIDStruct struct = null;
		try
		{
			struct = extractNameEmailId(userNameFile);
			
			if (struct.id == null)
			{
				struct.id = UUID.randomUUID().toString();
				userToCreate = struct;
				writeOutUserFile(userNameFile, struct);
			}
		}
		catch (IOException e)
		{
			throw new DBAbstractionException("problem handling user file",e);
		}
		
		return struct.id;
	}

	private static void writeOutUserFile(File file, UserNameIDStruct userStruct) throws IOException
	{
		
		//Clear out old file by deleting
		if (file.exists() && !file.delete())
		{
			throw new IOException("Could not clear out old file");
		}

		if (!file.createNewFile())
		{
			throw new IOException("Could not make the file");
		}
		
		try (FileOutputStream fos = new FileOutputStream(file);)
		{
			fos.write((userStruct.userName + "\n").getBytes());
			fos.write((userStruct.email + "\n").getBytes());
			if (userStruct.id != null)
			{
				fos.write((userStruct.id + "\n").getBytes());
			}
		}

	}

	private static UserNameIDStruct extractNameEmailId(File file) throws IOException
	{
		UserNameIDStruct struct = new UserNameIDStruct();
		try (Scanner scanner = new Scanner(file);)
		{
			assertTrue(scanner.hasNext());

			struct.userName = scanner.nextLine();
			struct.email = scanner.nextLine();
			if (scanner.hasNext())
			{
				struct.id = scanner.nextLine();
			}
		}
		return struct;
	}

	private static class UserNameIDStruct
	{
		String userName, email, id;
	}

	
	private static class MockRemoteSQLDatabase extends RemoteSQLDatabase
	{

		public MockRemoteSQLDatabase()
		{
			super("[MOCK]");
		}

		@Override
		public void close()
		{}

		@Override
		public String getUserId()
		{
			return "[MOCK]";
		}

		@Override
		public void storeToolUsage(ToolUsage tu, String associatedPlugin)
		{}

		@Override
		public void registerNewUser(String newUserEmail, String newUserName, String newUserId)
		{}

		@Override
		protected PreparedStatement makePreparedStatement(String statementQuery)
		{
			return null;
		}

		@Override
		protected void executeStatementWithNoResults(PreparedStatement statement)
		{}

		@Override
		protected ResultSet executeWithResults(PreparedStatement statement)
		{
			return null;
		}

		@Override
		protected Logger getLogger()
		{
			return null;
		}
		
	}
}
