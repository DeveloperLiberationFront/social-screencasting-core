package edu.ncsu.lubick.localHub.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class QueuedMySQLDatabase extends SQLDatabase {

	private static Logger logger = Logger.getLogger(QueuedMySQLDatabase.class.getName());
	private Connection connection;
	
	public QueuedMySQLDatabase()
	{
		try
		{
			loadDatabaseDriver();
		}
		catch (ClassNotFoundException e)
		{
			logger.fatal("Could not find driver for MySQLDatabase");
			throw new DBAbstractionException("Could not find driver for MySQLDatabase", e);
		}
		openRemoteConnection();
		}

	private void loadDatabaseDriver() throws ClassNotFoundException
	{
		Class.forName("com.mysql.jdbc.Driver");
	}
	
	private void openRemoteConnection()
	{
		if (this.connection != null)
		{
			return;
		}
		Connection newConnection = null;
		try
		{
			 newConnection = DriverManager.getConnection("jdbc:mysql://eb2-2291-fas01.csc.ncsu.edu:4747/screencast?user=screencast_user&password=screencast");
		}
		catch (SQLException e)
		{
			logger.error("Problem connecting to MySQLDatabase", e);
		}
		
		if (newConnection != null)
		{
			this.connection = newConnection;
		}
	}


	@Override
	public void close()
	{
		// TODO Auto-generated method stub

	}
	
	@Override
	protected void createTables()
	{
		//we shouldn't make the tables in the MySQL because the are remotely managed
	}

	@Override
	protected PreparedStatement makePreparedStatement(String statementQuery)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void executeStatementWithNoResults(PreparedStatement statement)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ResultSet executeWithResults(PreparedStatement statement)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
