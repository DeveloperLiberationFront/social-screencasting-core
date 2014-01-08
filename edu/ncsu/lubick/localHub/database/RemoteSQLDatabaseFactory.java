package edu.ncsu.lubick.localHub.database;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

public final class RemoteSQLDatabaseFactory
{

	private static final String PATH_TO_USER_FILE = "./username.txt";
	public static final String TEST_PATH_TO_USER_FILE = "./test_username.txt";

	private static boolean isTesting = false;
	
	private static UserNameIDStruct userToCreate = null;

	private static File expectedUserNameAndFile = new File(PATH_TO_USER_FILE);

	public static void setTestingMode(boolean b)
	{
		isTesting = b;
		if (b)
		{
			expectedUserNameAndFile = new File(TEST_PATH_TO_USER_FILE);
		}
		else
		{
			expectedUserNameAndFile = new File(PATH_TO_USER_FILE);
		}

	}

	public static RemoteSQLDatabase createMySQLDatabaseUsingUserFile()
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
		if (!isTesting)
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

}
