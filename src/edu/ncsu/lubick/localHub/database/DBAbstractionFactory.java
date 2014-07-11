package edu.ncsu.lubick.localHub.database;

import edu.ncsu.lubick.localHub.UserManager;

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

	public static ExternalDBAbstraction createAndInitializeExternalDatabase(UserManager um, boolean shouldActuallyConnect)
	{
		if (shouldActuallyConnect)
		{
			return ExternalDBFactory.createDatabase(um, ExternalDBFactory.MYSQL_IMPLEMENTATION);
		}
		return ExternalDBFactory.createDatabase(um, ExternalDBFactory.DUMMY_IMPLEMENTATION);
	}

}
