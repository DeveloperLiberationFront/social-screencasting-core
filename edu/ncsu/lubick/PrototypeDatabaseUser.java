package edu.ncsu.lubick;

import org.apache.log4j.PropertyConfigurator;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.database.QueuedMySQLDatabase;
import edu.ncsu.lubick.localHub.database.RemoteDBAbstraction;

public class PrototypeDatabaseUser {

	public static void main(String[] args)
	{
		PropertyConfigurator.configure(LocalHub.LOGGING_FILE_PATH);
		RemoteDBAbstraction db = new QueuedMySQLDatabase(null);
		
		System.out.println(db.registerNewUser("kjlubick@ncsu.edu", "Kevin Lubick"));
		
		db.close();
	}
	
}
