package edu.ncsu.lubick.localHub.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

public class QueuedMySQLDatabase extends SQLDatabase {

	private static final long TIME_BETWEEN_RECONNECTS = 30*1000;	//30 seconds for reconnects
	private static Logger logger = Logger.getLogger(QueuedMySQLDatabase.class.getName());
	private Connection connection;
	private Date lastConnectionAttemptTime;

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
		maybeTryConnectionReset();
	}

	private void loadDatabaseDriver() throws ClassNotFoundException
	{
		Class.forName("com.mysql.jdbc.Driver");
	}

	private boolean openRemoteConnection()
	{
		if (this.connection != null)
		{
			return false;
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
			return true;
		}
		return false;
	}


	@Override
	public void close()
	{
		if (connection != null)
		{
			try
			{
				connection.close();
			}
			catch (SQLException e)
			{
				throw new DBAbstractionException(e);
			}
		}
	}

	@Override
	protected void createTables()
	{
		//we shouldn't make the tables in the MySQL because they are remotely managed
	}

	@Override
	protected PreparedStatement makePreparedStatement(String statementQuery)
	{
		if (checkDatabaseConnection())
		{
			try
			{
				return connection.prepareStatement(statementQuery);
			}
			catch (SQLException e)
			{
				throw new DBAbstractionException(e);
			}
		}
		if (maybeTryConnectionReset())
		{
			return makePreparedStatement(statementQuery);
		}
		return new SerializablePreparedStatement(statementQuery);
	}


	private boolean maybeTryConnectionReset()
	{
		if (lastConnectionAttemptTime == null)
		{
			lastConnectionAttemptTime = new Date();
			return false;
		}
		if ((new Date().getTime() - lastConnectionAttemptTime.getTime()) > TIME_BETWEEN_RECONNECTS)
		{
			if (openRemoteConnection())
			{
				lastConnectionAttemptTime = null;	//successful connection
				return true;
			}
			lastConnectionAttemptTime = new Date();
		}
		return false;
	}

	private boolean checkDatabaseConnection()
	{
		try
		{
			if (connection != null && connection.isValid(1))
			{
				return true;
			}
			connection = null;
			return false;
		}
		catch (SQLException e)
		{
			logger.error("Problem validating connection", e);
			return false;
		}
	}

	@Override
	protected void executeStatementWithNoResults(PreparedStatement statement)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected ResultSet executeWithResults(PreparedStatement statement)
	{
		if (this.connection == null)
		{
			return null;
		}
		
		if (statement instanceof SerializablePreparedStatement)
		{
			if (checkDatabaseConnection())
			{
				return handleQueryWhileConnected((SerializablePreparedStatement)statement);
			}
			throw new DBAbstractionException("Can't perform queries when disconnected");
		}
		
		return handleQueryWhileConnected(statement);
		
	}

	private ResultSet handleQueryWhileConnected(PreparedStatement statement)
	{
		try
		{
			return statement.executeQuery();
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("Error processing connected query",e);
		}
	}
	
	private ResultSet handleQueryWhileConnected(SerializablePreparedStatement statement)
	{
		return statement.executeQuery(this.connection);
	}

}
