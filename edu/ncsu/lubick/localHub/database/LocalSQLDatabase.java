package edu.ncsu.lubick.localHub.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public abstract class LocalSQLDatabase extends LocalDBAbstraction {

	protected abstract PreparedStatement makePreparedStatement(String statementQuery);
	protected abstract void executeStatementWithNoResults(PreparedStatement statement);
	protected abstract ResultSet executeWithResults(PreparedStatement statement);

	protected void createTables()
	{
		createToolUsageTable();
		createClipTable();
	}



	private void createToolUsageTable()
	{
		/*
		 * ToolUsage Schema CREATE TABLE IF NOT EXISTS ToolUsages ( use_id INTEGER PRIMARY KEY AUTOINCREMENT plugin_name TEXT, usage_timestamp INTEGER,
		 * tool_name TEXT, tool_key_presses TEXT, class_of_tool TEXT, tool_use_duration INTEGER )
		 */
		// build up the sql
		String sqlTableQuery = 		"CREATE TABLE IF NOT EXISTS ToolUsages ( " +
				"use_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"plugin_name TEXT, " +
				"usage_timestamp INTEGER, " +
				"tool_name TEXT, " +
				"tool_key_presses TEXT, " +
				"class_of_tool TEXT, " +
				"tool_use_duration INTEGER," +
				"clip_score INTEGER" +
				") ";

		// execute the query
		PreparedStatement statement = makePreparedStatement(sqlTableQuery);
		executeStatementWithNoResults(statement);
	}

	private void createClipTable()
	{
		String sqlTableQuery = 		"CREATE TABLE IF NOT EXISTS Clips ( " +
				"folder_name TEXT PRIMARY KEY, " +
				"plugin_name TEXT, " +
				"tool_name TEXT, " +
				"clip_score INTEGER" +
				") ";

		// execute the query
		PreparedStatement statement = makePreparedStatement(sqlTableQuery);
		executeStatementWithNoResults(statement);
	}
	@Override
	public void storeToolUsage(ToolUsage tu, String associatedPlugin)
	{
		String sqlQuery = 		"INSERT INTO ToolUsages ( "+
				"plugin_name, "+
				"usage_timestamp, "+
				"tool_name, "+
				"tool_key_presses, "+
				"class_of_tool, "+
				"tool_use_duration,"+
				"clip_score  ) VALUES (?,?,?,?,?,?,?)";


		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, associatedPlugin);
			statement.setLong(2, tu.getTimeStamp().getTime());
			statement.setString(3, tu.getToolName());
			statement.setString(4, tu.getToolKeyPresses());
			statement.setString(5, tu.getToolClass());
			statement.setInt(6, tu.getDuration());
			statement.setInt(7, tu.getClipScore());
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
					int clipScore = results.getInt("clip_score");

					toolUsages.add(new ToolUsage(toolName, toolClass, keyPresses, currentPluginName, timestamp, duration, clipScore));
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
	public List<ToolUsage> getBestNInstancesOfToolUsage(int n, String pluginName, String toolName)
	{
		List<ToolUsage> toolUsages = new ArrayList<>();

		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM ToolUsages ");
		sqlQueryBuilder.append("WHERE plugin_name=? AND tool_name=? ORDER BY clip_score DESC");
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
				int clipScore = results.getInt("clip_score");

				toolUsages.add(new ToolUsage(toolName, toolClass, keyPresses, pluginName, timestamp, duration, clipScore));
			}

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}

		return toolUsages;
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

	@Override
	public List<Integer> getTopScoresForToolUsage(int maxToolUsages, String pluginName, String toolName)
	{
		List<Integer> scores = new ArrayList<>();

		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT clip_score FROM ToolUsages ");
		sqlQueryBuilder.append("WHERE plugin_name=? AND tool_name=? ORDER BY clip_score DESC");
		sqlQueryBuilder.append(" LIMIT "+maxToolUsages);

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
				int clipScore = results.getInt("clip_score");

				scores.add(clipScore);
			}

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}

		return scores;
	}

	/*
	 * "CREATE TABLE IF NOT EXISTS Clips ( " +
				"folder_name PRIMARY KEY, " +
				"plugin_name TEXT, " +
				"tool_name TEXT, " +
				"clip_score INTEGER" +
				") ";
	 */
	@Override
	public void createClipForToolUsage(String clipID, ToolUsage tu)
	{
		String sqlQuery = 		"INSERT INTO Clips ( "+
				"folder_name, "+
				"plugin_name, "+
				"tool_name, "+
				"clip_score  ) VALUES (?,?,?,?)";


		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, clipID);
			statement.setString(2, tu.getPluginName());
			statement.setString(3, tu.getToolName());
			statement.setInt(4, tu.getClipScore());
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem of the params in createClip()", e);
		}
	}

	@Override
	public void deleteClipForToolUsage(String clipID)
	{
		String sqlQuery = 		"DELETE FROM Clips where folder_name = ?";


		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, clipID);
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem in deleteClip()", e);
		}
	}

	private class PluginNameStruct {

		public String pluginName;
		public String toolName;

		public PluginNameStruct(String pluginName, String toolName)
		{
			this.pluginName = pluginName;
			this.toolName = toolName;
		}
		
	}
	
	@Override
	public List<String> getExcesiveTools()
	{
		String firstQuery = "SELECT plugin_name,tool_name from(" + 
				"SELECT plugin_name,tool_name, COUNT(*) AS num_clips FROM Clips Group by plugin_name, tool_name)" + 
				"WHERE num_clips > ?";
		
		List<PluginNameStruct> pluginToolCombosToThin = new ArrayList<>();
		
		try (PreparedStatement statement = makePreparedStatement(firstQuery);)
		{
			statement.setInt(1, LocalHub.MAX_TOOL_USAGES);
			
			try (ResultSet results = executeWithResults(statement);)
			{
				while (results.next())
				{
					pluginToolCombosToThin.add(new PluginNameStruct(results.getString(1),results.getString(2)));
				}
			}
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem in the first part of finding excess clips", e);
		}
		
		List<String> extrasToDelete = new ArrayList<>();
		for (PluginNameStruct pluginToolCombo : pluginToolCombosToThin)
		{
			findExtraClipsFrom(pluginToolCombo.pluginName, pluginToolCombo.toolName, extrasToDelete);
		}
		
		return extrasToDelete;
	}
	private void findExtraClipsFrom(String pluginName, String toolName, List<String> listToAppendTo)
	{
		String sqlQuery = "SELECT folder_name FROM Clips where plugin_name = ? AND tool_name = ? order by clip_score asc";
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, pluginName);
			statement.setString(1, toolName);
			
			try (ResultSet results = executeWithResults(statement);)
			{
				int numToDelete = results.getFetchSize()-LocalHub.MAX_TOOL_USAGES;
				for (int i = 0;i<numToDelete;i++)
				{
					listToAppendTo.add(results.getString(1));
				}
			}
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem in the first part of finding excess clips", e);
		}
		
	}

}
