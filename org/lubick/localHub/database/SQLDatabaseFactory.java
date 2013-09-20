package org.lubick.localHub.database;

public class SQLDatabaseFactory 
{

	public static final String DEFAULT_SQLITE_LOCATION = "./toolstreams.sqlite";
	
	public static final int SQLITE_IMPLEMENTATION = 10;

	public static DBAbstraction createDatabase(String databaseLocation,	int implementation) {
		
		if (implementation == SQLITE_IMPLEMENTATION)
		{
			return new SQLiteDatabase(databaseLocation);
		}
		return null;
	}
	
}
