package edu.ncsu.lubick;

import java.util.UUID;

import edu.ncsu.lubick.localHub.database.RemoteDBAbstraction;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;

public class PrototypeDatabaseUser {

	public static void main(String[] args)
	{
		TestingUtils.makeSureLoggingIsSetUp();
		RemoteDBAbstraction db = RemoteSQLDatabaseFactory.createMySQLDatabaseUsingUserFile();
		
		String newID = UUID.randomUUID().toString();
		db.registerNewUser("kjlubick@ncsu.edu", "Kevin Lubick", newID);
		System.out.println(newID);
		
		db.close();
	}
	
}
