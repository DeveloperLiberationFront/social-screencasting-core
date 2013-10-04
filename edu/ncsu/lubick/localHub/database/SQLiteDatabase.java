package edu.ncsu.lubick.localHub.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

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


	public void open(String path)
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


	private void createTables() {
		createToolUsageTable();

	}


	private void createToolUsageTable() throws DBAbstractionException
	{
		/*
		Event Schema
			CREATE TABLE IF NOT EXISTS ToolUsages (
				use_id INTEGER PRIMARY KEY AUTOINCREMENT,
				created_under_node_id TEXT PRIMARY_KEY,
				created_by_dev_group_id TEXT,
				node_sequence_num INTEGER PRIMARY_KEY,
				event_data TEXT,
				previous_neighbor_event_id TEXT,
				sequentially_before_event_id TEXT,
				event_type TEXT,
				paste_parent_id TEXT,
				document_id TEXT,
				deleted_at_timestamp INTEGER,
				deleted_by_created_by_dev_group_id TEXT,
				delete_event_id TEXT,
				directory_id TEXT,
				new_name TEXT,
				old_name TEXT,
				parent_directory_id TEXT,
				new_parent_directory_id TEXT,
				sequentially_before_node_id TEXT,
				first_node_to_merge_id TEXT,
				second_node_to_merge_id TEXT,
				base_resolution_id TEXT
			)
		 */
		//build up the sql
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE IF NOT EXISTS ToolUsages ( ");
		builder.append("use_id INTEGER PRIMARY KEY AUTOINCREMENT ");

		builder.append(") ");

		//execute the query
		executeWithNoResults(builder.toString());
	}


	//private helpers to perform queries
	private void executeWithNoResults(String sql)
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

	
	@SuppressWarnings("unused")
	private ResultSet executeWithResults(String sql) throws DBAbstractionException
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


	@Override
	public void storeToolUsage(ToolUsage tu, String associatedPlugin) {
		// TODO Auto-generated method stub
		adsf;lkasdfsoiekandkfand
	}
}
