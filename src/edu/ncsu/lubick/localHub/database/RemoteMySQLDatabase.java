package edu.ncsu.lubick.localHub.database;

import java.io.EOFException;
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
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;

public class RemoteMySQLDatabase implements ExternalDBAbstraction {

	private static final Logger logger = Logger.getLogger(RemoteMySQLDatabase.class);
	private static final long TIME_BETWEEN_RECONNECTS = 30*1000;	//30 seconds for reconnects
	
	private UserManager userManager;
	private Connection connection;
	private Date lastConnectionAttemptTime = new Date(0);

	private Queue<SerializablePreparedStatement> queuedStatements = new LinkedList<>();
	private File serializedStatementsFile;

	

	public RemoteMySQLDatabase(UserManager um)
	{
		this.userManager = um;
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
		
	}

	private void loadDatabaseDriver() throws ClassNotFoundException
	{
		logger.debug("Loading driver");
		Class.forName("com.mysql.jdbc.Driver");
	}

	private void loadQueuedStatements()
	{
		logger.debug("Loading previously queued files");
		this.serializedStatementsFile = new File("./dbStatic.sql");
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedStatementsFile));)
		{
			Object supposedQueue = ois.readObject();
			if (logger.isTraceEnabled()) logger.trace(supposedQueue);
			if (supposedQueue == null || !(supposedQueue instanceof Queue<?>))
			{
				logger.error("What I thought to be a queue was actually "+supposedQueue);
				return;
			}
			try {
				@SuppressWarnings("unchecked")
				Queue<SerializablePreparedStatement> tempQueue = (Queue<SerializablePreparedStatement>) supposedQueue;
				this.queuedStatements.addAll(tempQueue);
			}
			catch (ClassCastException e)
			{
				logger.error("Could not extract queue from disk",e);
			}
				
		}
		catch (FileNotFoundException e)
		{
			logger.debug(this.serializedStatementsFile + " did not exist");
			setupSerializedStatementsFile();
		}
		catch (EOFException e)
		{
			logger.info("Empty queued MySQL");
		}
		catch (IOException|ClassNotFoundException e)
		{
			throw new DBAbstractionException("Problem with the Serialized Statements File",e);
		}
		if (!this.serializedStatementsFile.delete())
		{
			logger.error("problem deleting the serialized statements file");
		}

	}


	private boolean maybeTryConnectionReset()
	{
		if (isTimeForNewAttempt())
		{
			if (openRemoteConnection())
			{
				logger.info("Connection to MySQL succeeded");
				lastConnectionAttemptTime = new Date(0);	//successful connection
				return true;
			}
			lastConnectionAttemptTime = new Date();
			logger.info("Connection to MySQL failed");
			return false;
		}
		logger.debug("Not attempting reconnect because the time isn't right yet");
		return false;
	}

	private boolean isTimeForNewAttempt()
	{
		return (new Date().getTime() - lastConnectionAttemptTime.getTime()) > TIME_BETWEEN_RECONNECTS;
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
			logger.error("Problem connecting to remote MySQLDatabase", e);
		}
	
		if (newConnection != null)
		{
			this.connection = newConnection;
			return true;
		}
		return false;
	}

	@Override
	public void storeToolUsage(ToolUsage tu)
	{
		String sql ="INSERT INTO tool_info_by_user (user_id, plugin_name, " 
				+"tool_name, usage_timestamp, tool_keypress, class_of_tool, "
				+"tool_use_duration ) VALUES (?,?,?,?,?,?,?)";


		try (PreparedStatement statement = makePreparedStatement(sql);)
		{
			statement.setString(1, this.userManager.getUserEmail());
			statement.setString(2, tu.getApplicationName());
			statement.setString(3, tu.getToolName());
			statement.setLong(4, tu.getTimeStamp().getTime());
			statement.setString(5, tu.getToolKeyPresses());
			statement.setString(6, tu.getToolClass());
			statement.setInt(7, tu.getDuration());
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem in storeToolUsage()", e);
		}
	}

	private void executeStatementWithNoResults(PreparedStatement statement)
	{
		if (statement instanceof SerializablePreparedStatement)
		{
			addToExecutionQueue((SerializablePreparedStatement) statement);
			if (checkDatabaseConnection() || maybeTryConnectionReset())
			{
				emptyExectutionQueue();
			}
		}
		else 
		{
			throw new DBAbstractionException("Statement was not prepared by the QueuedMySQLDatabase and is not usable here.");
		}
	}

	private void addToExecutionQueue(SerializablePreparedStatement statement)
	{
		this.queuedStatements.add(statement);
	}

	private void emptyExectutionQueue()
	{
		for(SerializablePreparedStatement s:queuedStatements)
		{
			handleExecutionWhileConnected(s);
		}
		queuedStatements.clear();
	}

	private void handleExecutionWhileConnected(SerializablePreparedStatement statement)
	{
		statement.executeUpdate(this.connection);

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

	private PreparedStatement makePreparedStatement(String statementQuery)
	{
		return new SerializablePreparedStatement(statementQuery);
	}

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
	public void connect()
	{
		maybeTryConnectionReset();
	}

}
