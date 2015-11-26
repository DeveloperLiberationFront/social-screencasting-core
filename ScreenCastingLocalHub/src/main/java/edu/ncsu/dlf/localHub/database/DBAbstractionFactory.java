package edu.ncsu.dlf.localHub.database;

import edu.ncsu.dlf.localHub.UserManager;

public class DBAbstractionFactory {

	public static final int SQL_IMPLEMENTATION = 1;

	public static LocalDBAbstraction createAndInitializeLocalDatabase(String databaseLocation, int implementation, UserManager um)
	{
		if (implementation == SQL_IMPLEMENTATION)
		{
			return LocalSQLDatabaseFactory.createDatabase(databaseLocation, LocalSQLDatabaseFactory.SQLITE_IMPLEMENTATION, um);
		}
		return null;
	}
}
