package edu.ncsu.lubick.localHub.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class SQLiteDatabase extends SQLDatabase
{

	private static Logger logger = Logger.getLogger(SQLiteDatabase.class.getName());
	private static final String DB_EXTENSION_NAME = ".sqlite";
	private String pathToFile;
	private Connection connection;
	private Statement previouslyExecutedStatement;

	public SQLiteDatabase(String databaseLocation)
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

	// helpers to perform queries
	@Override
	protected void executeWithNoResults(String sql)
	{
		logger.debug("Executing sql query (no results expected): " + sql);
		// create a statement
		try (Statement statement = connection.createStatement();)
		{
			// set timeout to 30 sec.
			statement.setQueryTimeout(30);

			// execute the query
			statement.executeUpdate(sql);
		}
		catch (SQLException e)
		{
			logger.error("Problem with Query Text: \n" + sql);

			// if something bad happened in the sql, wrap up the
			// exception and pass it on up
			throw new DBAbstractionException(e);
		}
	}

	@Override
	protected ResultSet executeWithResults(String sql)
	{
		logger.debug("Executing sql query for results: " + sql);
		ResultSet results = null;

		// create a statement
		try
		{
			previouslyExecutedStatement = connection.createStatement();
			// set timeout to 30 sec.
			previouslyExecutedStatement.setQueryTimeout(30);

			// execute the query and get back a generic set of results
			results = previouslyExecutedStatement.executeQuery(sql);

		}
		catch (SQLException e)
		{
			logger.error("Problem with Query Text: \n" + sql);

			throw new DBAbstractionException(e);
		}

		// Warning! Closing the statement before the results are used
		// invalidates the ResultSet
		return results;
	}

	@Override
	protected void cleanUpAfterQuery()
	{
		if (previouslyExecutedStatement != null)
			try
			{
				previouslyExecutedStatement.close();
			}
			catch (SQLException e)
			{
				logger.error("Problem closing statement", e);
			}
	}

}
