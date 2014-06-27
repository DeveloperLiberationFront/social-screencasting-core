package edu.ncsu.lubick.localHub.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ClipOptions;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

/**
 * 
 * @author Kevin
 *
 * History:
 * 2014/06/24 Slankas added table used to stage usages to central repositories
 * 
 */
public abstract class LocalSQLDatabase extends LocalDBAbstraction {
	
	protected abstract Logger getLogger();
	protected abstract String getUserEmail();

	protected abstract PreparedStatement makePreparedStatement(String statementQuery);
	protected abstract void executeStatementWithNoResults(PreparedStatement statement);
	protected abstract ResultSet executeWithResults(PreparedStatement statement);

	protected static final String[] TOOL_TABLE_NAMES = { "ToolUsages", "ToolUsagesStage" };
	
	protected void createTables()
	{
		createToolUsageTable();
		createClipTable();
		createDatabaseInfoTable();
	}


	/**
	 * creates the table used to store usages if it doesn't already exist.  
	 * Two tables are used.  The first contains the complete history of all tool usages from the current computer
	 * The second table is a stagin table used to send results to other repositories.
	 */
	private void createToolUsageTable()
	{
		for (String tableName: TOOL_TABLE_NAMES) {
			String sqlTableQuery = 		"CREATE TABLE IF NOT EXISTS "+ tableName+" ( " +
				"use_id TEXT PRIMARY KEY, " +
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
	}

	private void createClipTable()
	{
		String sqlTableQuery = 		"CREATE TABLE IF NOT EXISTS Clips ( " +
				"folder_name TEXT PRIMARY KEY, " +
				"plugin_name TEXT, " +
				"tool_name TEXT, " +
				"clip_score INTEGER," +
				"uploaded_date INTEGER," +
				"start_frame INTEGER," +
				"end_frame INTEGER," +
				"time_stamp LONG," +
				"start_data TEXT," +
				"end_data TEXT," +
				"rating_data TEXT" +
				") ";

		// execute the query
		PreparedStatement statement = makePreparedStatement(sqlTableQuery);
		executeStatementWithNoResults(statement);
	}
	
	private void createDatabaseInfoTable()
	{	
		String sqlTableQuery = "CREATE TABLE IF NOT EXISTS Database_Info ( " +
								"db_version INTEGER" +
								") ";
		
		PreparedStatement statement = makePreparedStatement(sqlTableQuery);
		executeStatementWithNoResults(statement);
	}
	
	@Override
	public void storeToolUsage(ToolUsage tu, String associatedPlugin)
	{
		for (String tableName: TOOL_TABLE_NAMES) {
			String sqlQuery = 		
					"INSERT INTO  "+ tableName+			
					   " ( use_id, plugin_name, usage_timestamp, tool_name, " +
					   "  tool_key_presses, class_of_tool, tool_use_duration,clip_score ) "+
					" VALUES (?,?,?,?,?,?,?,?)";
	
	
			try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
			{
				String uniqueId = ToolStream.makeUniqueIdentifierForToolUsage(tu, getUserEmail());
				statement.setString(1, uniqueId);
				statement.setString(2, associatedPlugin);
				statement.setLong(3, tu.getTimeStamp().getTime());
				statement.setString(4, tu.getToolName());
				statement.setString(5, tu.getToolKeyPresses());
				statement.setString(6, tu.getToolClass());
				statement.setInt(7, tu.getDuration());
				statement.setInt(8, tu.getClipScore());
				
				getLogger().debug(String.format("INSERT INTO "+tableName+" ( use_id, plugin_name, usage_timestamp, tool_name, tool_key_presses, class_of_tool, "+
					"tool_use_duration, clip_score  ) VALUES (%s,%s,%d,%s,%s,%s,%d,%d)",
					uniqueId,associatedPlugin, tu.getTimeStamp().getTime(), tu.getToolName(), tu.getToolKeyPresses(), tu.getToolClass(), tu.getDuration(), tu.getClipScore()));
				
				executeStatementWithNoResults(statement);
			}
			catch (SQLException e)
			{
				throw new DBAbstractionException("There was a problem in storeToolUsage()", e);
			}
		}

	}

	@Override
	public ToolUsage getToolUsageById(String folder)
	{
		String sqlQuery = "SELECT * FROM ToolUsages WHERE use_id = ?";
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, folder);

			try (ResultSet results = executeWithResults(statement);)
			{
				// perform the query
				if (results.next())
				{
					String toolName = results.getString("tool_name");
					String pluginName = results.getString("plugin_name");
					Date timestamp = new Date(results.getLong("usage_timestamp"));
					String toolClass = results.getString("class_of_tool");

					String keyPresses = results.getString("tool_key_presses");
					int duration = results.getInt("tool_use_duration");
					int clipScore = results.getInt("clip_score");
					

					return new ToolUsage(toolName, toolClass, keyPresses, pluginName, timestamp, duration, clipScore);
				}

			}
		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}

		return null;
		
	}
	
	@Override
	public ToolUsage getClipByFolder(String clipId)
	{
		String sqlQuery = "Select * FROM Clips WHERE folder_name = ?";
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, clipId);
			
			try (ResultSet results = executeWithResults(statement);)
			{
				if(results.next())
				{
					String toolName = results.getString("tool_name");
					String pluginName = results.getString("plugin_name");
					int score = results.getInt("clip_score");
					Date timeStamp = new Date(results.getLong("time_stamp"));
					String startData = results.getString("start_data");
					String endData = results.getString("end_data");
					String ratingData = results.getString("rating_data");
					
					
					ToolUsage tu = new ToolUsage(toolName, null, null, pluginName, timeStamp, 0, score);
					tu.setStartData(startData);
					tu.setEndData(endData);
					return tu;
				}
			}
		}
		catch(SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}
		
		return null;
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
					//int startFrame = results.getInt("start_frame");
					//int endFrame = results.getInt("end_frame");

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
	public List<ToolUsage> getBestNInstancesOfToolUsage(int n, String pluginName, String toolName, boolean isKeyboardShortcutHuh)
	{
		StringBuilder sqlQueryBuilder = new StringBuilder("SELECT * FROM ToolUsages "+
					"WHERE plugin_name=? AND tool_name=? AND clip_score > 0 AND ");
		
		if (isKeyboardShortcutHuh)
		{
			sqlQueryBuilder.append("tool_key_presses<>?");
		}
		else 
		{
			sqlQueryBuilder.append("tool_key_presses=?");
		}
		
		sqlQueryBuilder.append(" ORDER BY clip_score DESC LIMIT ");
		sqlQueryBuilder.append(n);

		PreparedStatement statement = makePreparedStatement(sqlQueryBuilder.toString());
		try
		{
			statement.setString(1, pluginName);
			statement.setString(2, toolName);
			statement.setString(3, ToolStream.MENU_KEY_PRESS);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem of the params in getLastNInstancesOfToolUsage()", e);
		}


		List<ToolUsage> toolUsages = new ArrayList<>();
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
				//int startFrame = results.getInt("start_frame");
				//int endFrame = results.getInt("end_frame");

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



	/*
	 * "CREATE TABLE IF NOT EXISTS Clips ( " +
				"folder_name PRIMARY KEY, " +
				"plugin_name TEXT, " +
				"tool_name TEXT, " +
				"clip_score INTEGER" +
				") ";
	 */
	@Override
	public void createClipForToolUsage(String clipID, ToolUsage tu, ClipOptions clipOptions)
	{
		String sqlQuery = 		"INSERT INTO Clips ( "+
				"folder_name, "+
				"plugin_name, "+
				"tool_name, "+
				"clip_score, "+
				"start_frame, "+
				"end_frame," +
				"time_stamp," +
				"start_data," +
				"end_data," +
				"rating_data" +
				") VALUES (?,?,?,?,?,?,?,?,?,?)";


		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, clipID);
			statement.setString(2, tu.getPluginName());
			statement.setString(3, tu.getToolName());
			statement.setInt(4, tu.getClipScore());
			statement.setInt(5, clipOptions.startFrame);
			statement.setInt(6, clipOptions.endFrame);
			statement.setLong(7, tu.getTimeStamp().getTime());
			
			String startData = tu.getStartData();
			if(startData == null)
				startData = "";
			String endData = tu.getEndData();
			if(endData == null)
				endData = "";
			
			String ratingData = tu.getRatingData();
			if(ratingData == null)
				ratingData = "";
			
			statement.setString(8, startData);
			statement.setString(9, endData);
			statement.setString(10, ratingData);
			
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
		String sqlQuery = "DELETE FROM Clips where folder_name = ?";


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

		List<PluginNameStruct> potentialPluginToolCombosToThin = new ArrayList<>();

		try (PreparedStatement statement = makePreparedStatement(firstQuery);)
		{
			statement.setInt(1, LocalHub.MAX_TOOL_USAGES);

			try (ResultSet results = executeWithResults(statement);)
			{
				while (results.next())
				{
					potentialPluginToolCombosToThin.add(new PluginNameStruct(results.getString(1),results.getString(2)));
				}
			}
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem in the first part of finding excess clips", e);
		}

		List<String> extrasToDelete = new ArrayList<>();
		for (PluginNameStruct pluginToolCombo : potentialPluginToolCombosToThin)
		{
			findExcessiveClipsFrom(pluginToolCombo.pluginName, pluginToolCombo.toolName, extrasToDelete);
		}

		return extrasToDelete;
	}
	private boolean isGUIClipPath(String clipPath)
	{
		return clipPath.endsWith("G");
	}
	private void findExcessiveClipsFrom(String pluginName, String toolName, List<String> listToAppendTo)
	{
		String sqlQuery = "SELECT folder_name FROM Clips where plugin_name = ? AND tool_name = ? order by clip_score desc";
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, pluginName);
			statement.setString(2, toolName);

			try (ResultSet results = executeWithResults(statement);)
			{				
				List<String> listOfClipPaths = makeClipPaths(results);
				
				findExcessiveClipTypes(listToAppendTo, listOfClipPaths);
			}
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem in the first part of finding excess clips", e);
		}

	}
	private void findExcessiveClipTypes(List<String> listToAppendTo, List<String> listOfClipPaths)
	{
		int numKeyboardShortcuts = 0;
		int numGUI = 0;
		for(String clipPath:listOfClipPaths) {
			if (isGUIClipPath(clipPath)) {
				numGUI++;
				if (numGUI > LocalHub.MAX_TOOL_USAGES){
					listToAppendTo.add(clipPath);
				}
			}
			else {
				numKeyboardShortcuts++;
				if (numKeyboardShortcuts > LocalHub.MAX_TOOL_USAGES) {
					listToAppendTo.add(clipPath);
				}
			}
		}
	}
	private List<String> makeClipPaths(ResultSet results) throws SQLException
	{
		List<String> listOfClipPaths = new ArrayList<>();
		while(results.next())
		{
			listOfClipPaths.add(results.getString(1));
		}
		return listOfClipPaths;
	}
	@Override
	public boolean isClipUploaded(String clipId)
	{
		String sqlQuery = "SELECT uploaded_date FROM Clips where folder_name LIKE ?";
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, clipId);

			try (ResultSet results = executeWithResults(statement);)
			{
				if(results.next())
				{
					return results.getLong(1) != 0;	//if the date is not set, it will return 0
				}

			}
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem checking if this clip was uploaded", e);
		}
		return false;
	}

	@Override
	public void setClipUploaded(String clipId, boolean b)
	{
		long uploadedDate = b?new Date().getTime():0;
		
		String sqlQuery = "UPDATE Clips SET uploaded_date = ? where folder_name LIKE ?";
		
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setLong(1, uploadedDate);
			statement.setString(2, PostProductionHandler.MEDIA_OUTPUT_FOLDER + clipId);
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem marking clip as uploaded or not", e);
		}
	}
	
	@Override
	public Boolean setStartEndFrame(String folder, int startFrame, int endFrame) {
		String sqlQuery = "UPDATE Clips SET start_frame = ?, end_frame = ? WHERE folder_name = ?";
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery))
		{
			statement.setInt(1, startFrame);
			statement.setInt(2, endFrame);
			statement.setString(3, folder);
			executeStatementWithNoResults(statement);
			return true;
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem changing the start or end frames", e);
		}
	}


}
