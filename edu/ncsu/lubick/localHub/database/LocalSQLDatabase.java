package edu.ncsu.lubick.localHub.database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public abstract class LocalSQLDatabase extends LocalDBAbstraction {

	protected abstract PreparedStatement makePreparedStatement(String statementQuery);
	protected abstract void executeStatementWithNoResults(PreparedStatement statement);
	protected abstract ResultSet executeWithResults(PreparedStatement statement);

	protected void createTables()
	{
		createToolUsageTable();
		createRawVideoCapFiles();
	}

	private void createRawVideoCapFiles()
	{
		/*
		 * RawVideoCapFiles Schema CREATE TABLE IF NOT EXISTS RawVideoCapFiles ( file_id INTEGER PRIMARY KEY AUTOINCREMENT file_name TEXT, video_start_time
		 * INTEGER, //The video's start time is only accurate to the nearest second duration INTEGER //This is in seconds
		 * 
		 * )
		 */
		// build up the sql
		StringBuilder sqlTableQueryBuilder = new StringBuilder();
		sqlTableQueryBuilder.append("CREATE TABLE IF NOT EXISTS RawVideoCapFiles ( ");
		sqlTableQueryBuilder.append("file_id INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sqlTableQueryBuilder.append("file_name TEXT, ");
		sqlTableQueryBuilder.append("video_start_time INTEGER, ");
		sqlTableQueryBuilder.append("duration INTEGER");
		sqlTableQueryBuilder.append(") ");

		// execute the query
		PreparedStatement statement = makePreparedStatement(sqlTableQueryBuilder.toString());
		executeStatementWithNoResults(statement);
	}

	private void createToolUsageTable()
	{
		/*
		 * ToolUsage Schema CREATE TABLE IF NOT EXISTS ToolUsages ( use_id INTEGER PRIMARY KEY AUTOINCREMENT plugin_name TEXT, usage_timestamp INTEGER,
		 * tool_name TEXT, tool_key_presses TEXT, class_of_tool TEXT, tool_use_duration INTEGER )
		 */
		// build up the sql
		StringBuilder sqlTableQueryBuilder = new StringBuilder();
		sqlTableQueryBuilder.append("CREATE TABLE IF NOT EXISTS ToolUsages ( ");
		sqlTableQueryBuilder.append("use_id INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sqlTableQueryBuilder.append("plugin_name TEXT, ");
		sqlTableQueryBuilder.append("usage_timestamp INTEGER, ");
		sqlTableQueryBuilder.append("tool_name TEXT, ");
		sqlTableQueryBuilder.append("tool_key_presses TEXT, ");
		sqlTableQueryBuilder.append("class_of_tool TEXT, ");
		sqlTableQueryBuilder.append("tool_use_duration INTEGER");
		sqlTableQueryBuilder.append(") ");

		// execute the query
		PreparedStatement statement = makePreparedStatement(sqlTableQueryBuilder.toString());
		executeStatementWithNoResults(statement);
	}

	@Override
	public void storeToolUsage(ToolUsage tu, String associatedPlugin)
	{
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("INSERT INTO ToolUsages ( ");
		sqlQueryBuilder.append("plugin_name, ");
		sqlQueryBuilder.append("usage_timestamp, ");
		sqlQueryBuilder.append("tool_name, ");
		sqlQueryBuilder.append("tool_key_presses, ");
		sqlQueryBuilder.append("class_of_tool, ");
		sqlQueryBuilder.append("tool_use_duration ) VALUES (?,?,?,?,?,?)");


		try (PreparedStatement statement = makePreparedStatement(sqlQueryBuilder.toString());)
		{
			statement.setString(1, associatedPlugin);
			statement.setLong(2, tu.getTimeStamp().getTime());
			statement.setString(3, tu.getToolName());
			statement.setString(4, tu.getToolKeyPresses());
			statement.setString(5, tu.getToolClass());
			statement.setInt(6, tu.getDuration());
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem of the params in storeToolUsage()", e);
		}

	}

	@Override
	public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName)
	{

		List<ToolUsage> toolUsages = new ArrayList<>();
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM ToolUsages ");
		sqlQueryBuilder.append("WHERE plugin_name=?");

		try (PreparedStatement statement = makePreparedStatement(sqlQueryBuilder.toString());)
		{
			statement.setString(1, currentPluginName);
			try (ResultSet results = executeWithResults(statement);)
			{
				while (results.next())
				{
					String toolName = results.getString("tool_name");
					Date timestamp = new Date(results.getLong("usage_timestamp"));
					String toolClass = results.getString("class_of_tool");

					String keyPresses = results.getString("tool_key_presses");
					int duration = results.getInt("tool_use_duration");

					toolUsages.add(new ToolUsage(toolName, toolClass, keyPresses, currentPluginName, timestamp, duration));
				}
			}
		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}

		return toolUsages;
	}

	@Override
	public void storeVideoFile(File newVideoFile, Date videoStartTime, int durationOfClip)
	{
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("INSERT INTO RawVideoCapFiles ( ");
		sqlQueryBuilder.append("file_name, ");
		sqlQueryBuilder.append("video_start_time, ");
		sqlQueryBuilder.append("duration ) VALUES (?,?,?)");

		
		try (PreparedStatement statement = makePreparedStatement(sqlQueryBuilder.toString());)
		{
			statement.setString(1, newVideoFile.getAbsolutePath());
			statement.setLong(2, videoStartTime.getTime() / 1000);
			statement.setInt(3, durationOfClip);
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem of the params in storeVideoFile()", e);
		}

		

	}


	@Override
	public List<ToolUsage> getLastNInstancesOfToolUsage(int n, String pluginName, String toolName)
	{
		List<ToolUsage> toolUsages = new ArrayList<>();

		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM ToolUsages ");
		sqlQueryBuilder.append("WHERE plugin_name=? AND tool_name=? ORDER BY usage_timestamp DESC");
		sqlQueryBuilder.append(" LIMIT "+n);

		PreparedStatement statement = makePreparedStatement(sqlQueryBuilder.toString());
		try
		{
			statement.setString(1, pluginName);
			statement.setString(2, toolName);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem of the params in getLastNInstancesOfToolUsage()", e);
		}


		try (ResultSet results = executeWithResults(statement);)
		{
			// perform the query
			while (results.next())
			{
				Date timestamp = new Date(results.getLong("usage_timestamp"));
				String toolClass = results.getString("class_of_tool");

				String keyPresses = results.getString("tool_key_presses");
				int duration = results.getInt("tool_use_duration");

				toolUsages.add(new ToolUsage(toolName, toolClass, keyPresses, pluginName, timestamp, duration));
			}

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}

		return toolUsages;
	}

	/*
	 * RawVideoCapFiles Schema CREATE TABLE IF NOT EXISTS RawVideoCapFiles ( file_id INTEGER PRIMARY KEY AUTOINCREMENT file_name TEXT, video_start_time INTEGER,
	 * //The video's start time is only accurate to the nearest second duration INTEGER //This is in seconds
	 * 
	 * )
	 */

	@Override
	public List<FileDateStructs> getVideoFilesLinkedToTimePeriod(Date timeStamp, int durationInSeconds)
	{
		List<FileDateStructs> retVal = new ArrayList<>();

		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT DISTINCT file_name, video_start_time FROM RawVideoCapFiles ");
		sqlQueryBuilder.append("WHERE (video_start_time<");
		sqlQueryBuilder.append(timeStamp.getTime() / 1000);
		sqlQueryBuilder.append(" AND video_start_time+duration>");
		sqlQueryBuilder.append(timeStamp.getTime() / 1000);
		sqlQueryBuilder.append(") OR ( video_start_time<");
		sqlQueryBuilder.append(timeStamp.getTime() / 1000L + durationInSeconds);
		sqlQueryBuilder.append(" AND video_start_time+duration>");
		sqlQueryBuilder.append(timeStamp.getTime() / 1000L + durationInSeconds);
		sqlQueryBuilder.append(") ORDER BY video_start_time");

		PreparedStatement statement = makePreparedStatement(sqlQueryBuilder.toString());	//no need to add params
		//we trust this data

		try (ResultSet results = executeWithResults(statement);)
		{
			// perform the query
			while (results.next())
			{
				Date videoTimestamp = new Date(results.getLong("video_start_time") * 1000); // multiply by 1000 to
				// convert from seconds to millis
				File file = new File(results.getString("file_name"));

				FileDateStructs newResult = new FileDateStructs(file, videoTimestamp);

				retVal.add(newResult);
			}

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}

		return retVal;
	}

	@Override
	public List<String> getNamesOfAllPlugins()
	{
		List<String> retVal = new ArrayList<String>();

		String sqlQuery = "SELECT DISTINCT plugin_name FROM ToolUsages";
		PreparedStatement statement = makePreparedStatement(sqlQuery);

		try (ResultSet results = executeWithResults(statement);)
		{
			// perform the query
			while (results.next())
			{
				String plugin_name = results.getString("plugin_name");

				retVal.add(plugin_name);
			}

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}
		return retVal;
	}

}
