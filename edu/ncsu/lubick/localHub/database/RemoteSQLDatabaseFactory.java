package edu.ncsu.lubick.localHub.database;

import java.io.File;

public final class RemoteSQLDatabaseFactory 
{

	private static final String PATH_TO_USER_FILE = "./username.txt";
	private static final String TEST_PATH_TO_USER_FILE = "./test_username.txt";
	
	private static File expectedUserNameAndFile = new File(PATH_TO_USER_FILE);
	public static void setTestingMode(boolean b)
	{
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
			userId = createDummyUser(expectedUserNameAndFile);
		}
		return new QueuedMySQLDatabase(userId);
	}

	private static String createDummyUser(File expectedUserNameAndFile)
	{
		// TODO Auto-generated method stub
		return null;
	}

	private static String readOrCreateNewUserId(File userNameFile)
	{
		// TODO Auto-generated method stub
		return null;
	}



	
	
}
