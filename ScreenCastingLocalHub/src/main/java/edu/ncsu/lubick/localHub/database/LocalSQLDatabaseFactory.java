package edu.ncsu.lubick.localHub.database;

import edu.ncsu.lubick.localHub.UserManager;

public class LocalSQLDatabaseFactory
{

	public static final String DEFAULT_SQLITE_LOCATION = "./toolstreams.sqlite";

	public static final int SQLITE_IMPLEMENTATION = 10;

	public static LocalDBAbstraction createDatabase(String databaseLocation, int implementation, UserManager um)
	{

		if (implementation == SQLITE_IMPLEMENTATION)
		{
			return new LocalSQLiteDatabase(databaseLocation, um);
		}
		return null;
	}

}
