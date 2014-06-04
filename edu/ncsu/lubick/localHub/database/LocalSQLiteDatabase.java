package edu.ncsu.lubick.localHub.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class LocalSQLiteDatabase extends LocalSQLDatabase
{

	private static final Logger logger = Logger.getLogger(LocalSQLiteDatabase.class);
	private static final String DB_EXTENSION_NAME = ".sqlite";
	private Connection connection;
	private UserManager userManager;

	public LocalSQLiteDatabase(String databaseLocation, UserManager um)
	{
		this.userManager = um;
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

		//XXX remove from future releases
		try
		{
			patchDatabase();
		}
		catch (SQLException e)
		{
			logger.fatal("error patching",e);
		}
	}

	private final void patchDatabase() throws SQLException
	{
		//check to see if patch already done

		String alreadyPatchedString = "Select count(*) from ToolUsages where clip_score = -1";
		try (ResultSet results = executeWithResults(makePreparedStatement(alreadyPatchedString));)
		{
			if (results.next()) {
				if (results.getInt(1) > 0) {
					logger.info("Already patched database");
					return;
				}
			}
		}
		logger.info("Patching database - finding deleted folders");

		String sqlQueryString = "SELECT use_id FROM ToolUsages ";
		List<String> keysToEraseList = new ArrayList<>();
		try(PreparedStatement existantStatement = makePreparedStatement(sqlQueryString);) {
			File mediaOutput = new File(PostProductionHandler.MEDIA_OUTPUT_FOLDER);
			
			try (ResultSet results = executeWithResults(existantStatement);)
			{
				while(results.next()) {
					String clipId = results.getString(1);
					File f = new File(mediaOutput, clipId);
					if (!f.exists()) {
						keysToEraseList.add(clipId);
					}
				}
			}
		}
		logger.info("Patching database - updating database");

		String resetQueryString = "UPDATE ToolUsages SET clip_score = -1 where use_id = ?";
		try (PreparedStatement resetScoreStatement = makePreparedStatement(resetQueryString);) {
			for(String useId : keysToEraseList) {

				resetScoreStatement.setString(1, useId);
				resetScoreStatement.addBatch();
			}
			logger.info("Patching database - executing batch");

			resetScoreStatement.executeBatch();
		}

		logger.info("Finished patch");
	}

	private final void open(String path)
	{
		try
		{
			// load the sqlite-JDBC driver using the class loader
			Class.forName("org.sqlite.JDBC");

			// create a database connection, will open the sqlite db if it
			// exists and create a new sqlite database if it does not exist
			this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);

			// create the tables (if they do not already exist) 
			createTables();
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

}
