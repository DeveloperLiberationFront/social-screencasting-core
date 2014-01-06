package edu.ncsu.lubick.localHub.database;

public class DBAbstractionFactory {

	public static final int SQL_IMPLEMENTATION = 1;

	public static LocalDBAbstraction createAndInitializeDatabase(String databaseLocation, int implementation)
	{
		if (implementation == SQL_IMPLEMENTATION)
		{
			return LocalSQLDatabaseFactory.createDatabase(databaseLocation, LocalSQLDatabaseFactory.SQLITE_IMPLEMENTATION);
		}
		return null;
	}

}
