package edu.ncsu.lubick.localHub.database;


public class DBAbstractionFactory {

	public static final int SQL_IMPLEMENTATION = 1;

	public static DBAbstraction createDatabase(String databaseLocation,	int implementation) 
	{
		if (implementation == SQL_IMPLEMENTATION)
		{
			return SQLDatabaseFactory.createDatabase(databaseLocation, SQLDatabaseFactory.SQLITE_IMPLEMENTATION);
		}
		return null;
	}

}
