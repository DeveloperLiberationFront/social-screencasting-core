package edu.ncsu.lubick.localHub.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.UserManager;

public class LocalSQLiteDatabase extends LocalSQLDatabase
{

	private static final Logger logger = Logger.getLogger(LocalSQLiteDatabase.class);
	private static final String DB_EXTENSION_NAME = ".sqlite";
	private Connection connection;
	private UserManager userManager;
	private String path;

	public LocalSQLiteDatabase(String databaseLocation, UserManager um)
	{
		this.userManager = um;
		this.path = databaseLocation;
		// check the filename has the right extension
		if (databaseLocation.endsWith(DB_EXTENSION_NAME))
		{
			logger.debug("Creating database at location: " + databaseLocation);
			// open a connection to a db so that this server can access the db
			open();
			
			renameTablesIfNeeded();
			// create the tables (if they do not already exist) 
			createTables();
		}
		else
			// incorrect file name
		{
			throw new DBAbstractionException("The database file name must end with " + DB_EXTENSION_NAME + " : " + databaseLocation);
		}

		updateDatabaseToNewest();
	}
	
	@SuppressWarnings("resource")		//Eclipse thinks firstStatement, secondStatement and so on need tryWithResources
	private void renameTablesIfNeeded()
	{
		int dbVersion = getDbVersion();
		if (dbVersion > 0 && dbVersion < 17)
		{
			String first =	"CREATE TABLE IF NOT EXISTS RenderedClips ( " +
					"folder_name TEXT PRIMARY KEY, " +
					"plugin_name TEXT, " +
					"tool_name TEXT, " +
					"clip_score INTEGER," +
					"uploaded_date INTEGER);";


			String second = "INSERT INTO RenderedClips SELECT folder_name,plugin_name,tool_name,"+
					"clip_score,uploaded_date FROM Clips;";
			String third = "DROP TABLE Clips;";

			
			try {
				PreparedStatement firstStatement = makePreparedStatement(first);
				executeStatementWithNoResults(firstStatement);
				firstStatement.close();
				PreparedStatement secondStatement = makePreparedStatement(second);
				executeStatementWithNoResults(secondStatement);
				secondStatement.close();
				//Needed to prevent database locking problems
				resetConnection();
				
				
				PreparedStatement thirdStatement = makePreparedStatement(third);
				executeStatementWithNoResults(thirdStatement);
				thirdStatement.close();
			}
			catch (SQLException e) {
				logger.error("Could not rename Clips table",e);
			}
		}
		
	}

	private final void updateDatabaseToNewest()
	{
		int dbVersion = getDbVersion();
		logger.debug("Database version: " + dbVersion);
		
		updateTo1_5(dbVersion, 15);
		updateTo1_6(dbVersion, 16);
		updateTo1_7(dbVersion, 17);
		
		storeDbVersion(dbVersion, 17);
	}
	
	/**
	 * 1.5 added the database version to the tables.
	 */
	private void updateTo1_5(int currentVersion, int newVersion)
	{
		if(currentVersion < newVersion)
		{
			String sqlQuery = "INSERT INTO Database_Info (" +
							  "db_version)  VALUES (?)";
			PreparedStatement statement = makePreparedStatement(sqlQuery);
			
			try
			{
				statement.setInt(1, newVersion);
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			executeStatementWithNoResults(statement);			
		}
	}
	
	private void updateTo1_6(int currentVersion, int newVersion)
	{
		if(currentVersion < newVersion)
		{
			if(!doesColumnExist("Clips", "start_frame"))
			{
				String addStartFrameToClip = "ALTER TABLE Clips ADD COLUMN start_frame INTEGER DEFAULT 0";
				PreparedStatement addStartFrameToClipStatement = makePreparedStatement(addStartFrameToClip);
				executeStatementWithNoResults(addStartFrameToClipStatement);
			}
			
			if(!doesColumnExist("Clips", "end_frame"))
			{
				String addEndFrameToClip   = "ALTER TABLE Clips ADD COLUMN end_frame INTEGER DEFAULT 0";
				PreparedStatement addEndFrameToClipStatement = makePreparedStatement(addEndFrameToClip);
				executeStatementWithNoResults(addEndFrameToClipStatement);
			}
			
			if(!doesColumnExist("Clips", "rating_data"))
			{
				String addRatingDataToClip = "ALTER TABLE CLIPS ADD COLUMN rating_data TEXT DEFAULT ''";
				PreparedStatement addRatingDataToClipStatement = makePreparedStatement(addRatingDataToClip);
				executeStatementWithNoResults(addRatingDataToClipStatement);
			}
		}
	}
	
	private void updateTo1_7(int currentVersion, int newVersion) {
		if(currentVersion < newVersion) {
			//nothing, we are renaming/moving/shrinking the database table.
		}
	}
	
	private void storeDbVersion(int currentVersion, int newVersion)
	{	
		if(currentVersion < newVersion)
		{
			logger.info("Updating database to version " + newVersion);
			
			PreparedStatement statement = makePreparedStatement("UPDATE Database_Info SET db_version = ? WHERE rowid = 1");
			
			try
			{
				statement.setInt(1, newVersion);
			}
			catch (SQLException e)
			{
				logger.error("Problem setting up dbVersionQuery",e);
			}
			executeStatementWithNoResults(statement);
		}
	}
	
	private int getDbVersion()
	{
		try
		{
			String sqlQuery = "SELECT db_version FROM Database_Info";
			PreparedStatement statement = makePreparedStatement(sqlQuery);
			ResultSet results = executeWithResults(statement);
			if(results.next())
			{
				return results.getInt("db_version");
			}	
		}
		catch (DBAbstractionException | SQLException e)
		{
			logger.error("Could not get database version", e);
		}		
		return 0;
	}
	
	private boolean doesColumnExist(String table, String column)
	{
		String sqlQuery = "SELECT " + column + " FROM " + table;
		try
		{
			makePreparedStatement(sqlQuery);
		}
		catch(DBAbstractionException e)
		{
			return false;
		}
		return true;
	}

	private final void open()
	{
		try
		{
			// load the sqlite-JDBC driver using the class loader
			Class.forName("org.sqlite.JDBC");

			// create a database connection, will open the sqlite db if it
			// exists and create a new sqlite database if it does not exist
			this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.path);

		}
		catch (ClassNotFoundException e)
		{
			throw new DBAbstractionException("Problem with Class.forName in SQLiteDatabase", e);
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
			throw new DBAbstractionException("Problem executing statement ",e);
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

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

	@Override
	protected String getUserEmail()
	{
		return userManager.getUserEmail();
	}

	private void resetConnection() throws SQLException
	{
		connection.setAutoCommit(false);
		connection.commit();
		connection.close();
		
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
	}

}
