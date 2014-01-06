package edu.ncsu.lubick.localHub.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

public class QueuedMySQLDatabase extends RemoteSQLDatabase{

	private static final long TIME_BETWEEN_RECONNECTS = 30*1000;	//30 seconds for reconnects
	private static Logger logger = Logger.getLogger(QueuedMySQLDatabase.class.getName());
	private Connection connection;
	private Date lastConnectionAttemptTime;
	
	private Queue<SerializablePreparedStatement> queuedStatements = new LinkedList<>();
	private File serializedStatementsFile;

	public QueuedMySQLDatabase(String userId)
	{
		super(userId);
		try
		{
			loadDatabaseDriver();
		}
		catch (ClassNotFoundException e)
		{
			logger.fatal("Could not find driver for MySQLDatabase");
			throw new DBAbstractionException("Could not find driver for MySQLDatabase", e);
		}
		loadQueuedStatements();
		maybeTryConnectionReset();
	}

	private void loadDatabaseDriver() throws ClassNotFoundException
	{
		Class.forName("com.mysql.jdbc.Driver");
	}
	
	private void loadQueuedStatements()
	{
		this.serializedStatementsFile = new File("./dbStatic.sql");
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedStatementsFile));)
		{
			Object supposedQueue = ois.readObject();
			if (supposedQueue == null || !(supposedQueue instanceof Queue<?>))
			{
				return;
			}
			extractObjectToExecutionQueue(supposedQueue);
		}
		catch (FileNotFoundException e)
		{
			setupSerializedStatementsFile();
		}
		catch (IOException|ClassNotFoundException e)
		{
			throw new DBAbstractionException("Problem with the Serialized Statements File",e);
		}

	}

	@SuppressWarnings("unchecked")
	private void extractObjectToExecutionQueue(Object supposedQueue)
	{
		Queue<SerializablePreparedStatement> tempQueue = (Queue<SerializablePreparedStatement>) supposedQueue;
		this.queuedStatements.addAll(tempQueue);
	}

	private void setupSerializedStatementsFile()
	{
		try
		{
			if (!this.serializedStatementsFile.createNewFile())
			{
				logger.error("Problem making "+this.serializedStatementsFile +" file");
			}
		}
		catch (IOException e)
		{
			throw new DBAbstractionException("Could not create "+this.serializedStatementsFile +" file", e);
		}
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
		writeExecutionQueueToDisk();
	}

	private void writeExecutionQueueToDisk()
	{
		try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializedStatementsFile));)
		{
			oos.writeObject(queuedStatements);
		}
		catch (IOException e)
		{
			logger.fatal("Could not save Execution Queue to Disk", e);
		}
		
	}

	@Override
	protected PreparedStatement makePreparedStatement(String statementQuery)
	{
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
		if (statement instanceof SerializablePreparedStatement)
		{
			addToExecutionQueue((SerializablePreparedStatement) statement);
			if (checkDatabaseConnection())
			{
				emptyExectutionQueue();
			}
		}
		else 
		{
			throw new DBAbstractionException("Statement was not prepared by the QueuedMySQLDatabase and is not usable here.");
		}
	}



	private void emptyExectutionQueue()
	{
		for(SerializablePreparedStatement s:queuedStatements)
		{
			handleExecutionWhileConnected(s);
		}
		queuedStatements.clear();
	}

	private void addToExecutionQueue(SerializablePreparedStatement statement)
	{
		this.queuedStatements.add(statement);
	}

	@Override
	protected ResultSet executeWithResults(PreparedStatement statement)
	{
		if (statement instanceof SerializablePreparedStatement)
		{
			if (checkDatabaseConnection())
			{
				return handleQueryWhileConnected((SerializablePreparedStatement)statement);
			}
			throw new DBAbstractionException("Can't perform queries when disconnected");
		}
		
		throw new DBAbstractionException("Statement was not prepared by the QueuedMySQLDatabase and is not usable here.");
		
	}
	
	private ResultSet handleQueryWhileConnected(SerializablePreparedStatement statement)
	{
		return statement.executeQuery(this.connection);
	}
	
	private void handleExecutionWhileConnected(SerializablePreparedStatement statement)
	{
		statement.executeUpdate(this.connection);
		
	}

}
