package edu.ncsu.lubick.localHub.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.LocalHub;

public class SQLiteDatabase extends SQLDatabase 
{

	private static Logger logger = Logger.getLogger(LocalHub.class.getName());
	private static final String DB_EXTENSION_NAME = ".sqlite";
	private String pathToFile;
	private Connection connection;

	public SQLiteDatabase(String databaseLocation) 
	{
		//check the filename has the right extension
		if(databaseLocation.endsWith(DB_EXTENSION_NAME))
		{
			//open a connection to a db so that this server can access the db
			open(databaseLocation);
		}
		else //incorrect file name
		{
			throw new DBAbstractionException("The database file name must end with " + DB_EXTENSION_NAME +" : "+databaseLocation);
		}
	}


	private void open(String path)
	{
		try
		{
			//load the sqlite-JDBC driver using the class loader
			Class.forName("org.sqlite.JDBC");

			//set the path to the sqlite database file
			this.pathToFile = path;

			//create a database connection, will open the sqlite db if it
			//exists and create a new sqlite database if it does not exist
			this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.pathToFile);

			//create the tables (if they do not already exist)
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


	//helpers to perform queries
	@Override
	protected void executeWithNoResults(String sql)
	{
		//create a statement
		try (Statement statement = connection.createStatement();)
		{
			// set timeout to 30 sec.
			statement.setQueryTimeout(30);

			//execute the query
			statement.executeUpdate(sql);
		}
		catch (SQLException e)
		{
			logger.error("Problem with Query Text: \n"+ sql);

			//if something bad happened in the sql, wrap up the
			//exception and pass it on up
			throw new DBAbstractionException(e);
		}
	}

	@Override
	protected ResultSet executeWithResults(String sql)
	{
		ResultSet results = null;

		//create a statement
		try(Statement statement = connection.createStatement();)
		{
			// set timeout to 30 sec.
			statement.setQueryTimeout(30);

			//execute the query and get back a generic set of results
			results = statement.executeQuery(sql);

		}
		catch (SQLException e)
		{
			logger.error("Problem with Query Text: \n"+ sql);
			throw new DBAbstractionException(e);
		}

		return results;
	}
}
