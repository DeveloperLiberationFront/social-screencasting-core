package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.localHub.database.QueuedMySQLDatabase;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;

public class TestMySQLDatabase 
{
	
	

	@BeforeClass
	public static void setUpClass() throws Exception
	{
		RemoteSQLDatabaseFactory.setTestingMode();
	}

	@Test
	public void test()
	{
		
		QueuedMySQLDatabase db = (QueuedMySQLDatabase) RemoteSQLDatabaseFactory.createMySQLDatabaseUsingUserFile();
		assertNotNull(db);
		assertTrue(db.isConnected());
		
		
	}
	
	
	private static class UserNameIDStruct
	{
		String userName, email, id;
	}

}
