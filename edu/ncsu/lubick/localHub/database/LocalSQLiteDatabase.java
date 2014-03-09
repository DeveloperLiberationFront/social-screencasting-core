package edu.ncsu.lubick.localHub.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class LocalSQLiteDatabase extends LocalSQLDatabase
{
	private static Logger logger = Logger.getLogger(LocalSQLiteDatabase.class.getName());
	private static final String DB_EXTENSION_NAME = ".sqlite";
	private String pathToFile;
	private Connection connection;

	public LocalSQLiteDatabase(String databaseLocation)
	{
		// check the filename has the right extension
		if (databaseLocation.endsWith(DB_EXTENSION_NAME))
		{
			logger.debug("Creating database at location: " + databaseLocation);
			// open a connection to a db so that this server can access the db
			open(databaseLocation);
		}
		else
		// incorrect file name
		{
			throw new DBAbstractionException("The database file name must end with " + DB_EXTENSION_NAME + " : " + databaseLocation);
		}
	}

	private void open(String path)
	{
		try
		{
			// load the sqlite-JDBC driver using the class loader
			Class.forName("org.sqlite.JDBC");

			// set the path to the sqlite database file
			this.pathToFile = path;

			// create a database connection, will open the sqlite db if it
			// exists and create a new sqlite database if it does not exist
			this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.pathToFile);

			// create the tables (if they do not already exist)
			createTables();
		}
		catch (ClassNotFoundException e)
		{
			throw new DBAbstractionException("Problem with Class.forName in SQLiteDatabase");
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException(e);
		}
	}

	@Override
	public void close()
	{
		try
		{
			// close the JDBC connection
			connection.close();

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}
	}

	@Override
	protected PreparedStatement makePreparedStatement(String statementQuery)
	{
		try
		{
			return connection.prepareStatement(statementQuery);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("Problem compiling SQL to preparedStatement",e);
		}
	}

	@Override
	protected void executeStatementWithNoResults(PreparedStatement statement)
	{
		try
		{
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("Problem executing statement",e);
		}
		
	}

	@Override
	protected ResultSet executeWithResults(PreparedStatement statement)
	{
		ResultSet retVal = null;
		try
		{
			retVal = statement.executeQuery();
			// statement.closeOnCompletion();  //Not supported by current version of JDBC		

		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("Problem with query", e);
		}
		return retVal;
		
	}


}
