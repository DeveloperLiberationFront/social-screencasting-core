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
import edu.ncsu.lubick.util.ToolCountStruct;

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
		String sqlTableQuery = 		"CREATE TABLE IF NOT EXISTS RenderedClips ( " +
				"folder_name TEXT PRIMARY KEY, " +
				"plugin_name TEXT, " +
				"tool_name TEXT, " +
				"clip_score INTEGER," +
				"uploaded_date INTEGER" +
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
				statement.setInt(8, tu.getUsageScore());
				
				getLogger().debug(String.format("INSERT INTO "+tableName+" ( use_id, plugin_name, usage_timestamp, tool_name, tool_key_presses, class_of_tool, "+
					"tool_use_duration, clip_score  ) VALUES (%s,%s,%d,%s,%s,%s,%d,%d)",
					uniqueId,associatedPlugin, tu.getTimeStamp().getTime(), tu.getToolName(), tu.getToolKeyPresses(), tu.getToolClass(), tu.getDuration(), tu.getUsageScore()));
				
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
	public ToolCountStruct getToolAggregate(String applicationName, String toolName) {
		
		//First, find the total number of tool uses that match this application/tool 
		String sqlQuery = "SELECT count(*) FROM ToolUsages WHERE plugin_name=? AND "
				+ "tool_name=?";

		int totalCount = 0;
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);){
			statement.setString(1, applicationName);
			statement.setString(2, toolName);
			try (ResultSet results = executeWithResults(statement);) {
				if (results.next()){
					totalCount = results.getInt(1);
				}
			}
		}catch (SQLException ex){
			throw new DBAbstractionException(ex);
		}
		
		//second, find the total number of gui uses that match this application/tool 
		sqlQuery = "SELECT count(*) FROM ToolUsages WHERE plugin_name=? AND "
				+ "tool_name=? AND tool_key_presses='GUI'";

		int guiCount = 0;
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);){
			statement.setString(1, applicationName);
			statement.setString(2, toolName);
			try (ResultSet results = executeWithResults(statement);) {
				if (results.next()){
					guiCount = results.getInt(1);
				}
			}
		}catch (SQLException ex){
			throw new DBAbstractionException(ex);
		}
		
		return new ToolCountStruct(toolName, guiCount, totalCount-guiCount);
	}

	@Override
	public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName)
	{

		List<ToolUsage> toolUsages = new ArrayList<>();
		String sqlQuery = "SELECT * FROM ToolUsages WHERE plugin_name=?";

		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
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
	 "CREATE TABLE IF NOT EXISTS RenderedClips ( " +
				"folder_name TEXT PRIMARY KEY, " +
				"plugin_name TEXT, " +
				"tool_name TEXT, " +
				"clip_score INTEGER," +
				"uploaded_date INTEGER" +
				") "
	 */
	@Override
	public void createClipForToolUsage(String clipID, ToolUsage tu, ClipOptions clipOptions)
	{
		String sqlQuery = 		"INSERT INTO RenderedClips ( "+
				"folder_name, "+
				"plugin_name, "+
				"tool_name, "+
				"clip_score, "+
				") VALUES (?,?,?,?)";


		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, clipID);
			statement.setString(2, tu.getPluginName());
			statement.setString(3, tu.getToolName());
			statement.setInt(4, tu.getUsageScore());
			
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
		String sqlQuery = "DELETE FROM RenderedClips where folder_name = ?";


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
				"SELECT plugin_name,tool_name, COUNT(*) AS num_clips FROM RenderedClips Group by plugin_name, tool_name)" + 
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
		String sqlQuery = "SELECT folder_name FROM RenderedClips where plugin_name = ? AND tool_name = ? order by clip_score desc";
		
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
		String sqlQuery = "SELECT uploaded_date FROM RenderedClips where folder_name LIKE ?";
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setString(1, "%"+clipId);

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
			throw new DBAbstractionException("There was a problem checking if this clip was uploaded "+clipId, e);
		}
		return false;
	}

	@Override
	public void setClipUploaded(String clipId, boolean b)
	{
		long uploadedDate = b ? new Date().getTime() : 0;
		
		String sqlQuery = "UPDATE RenderedClips SET uploaded_date = ? where folder_name LIKE ?";
		
		
		try (PreparedStatement statement = makePreparedStatement(sqlQuery);)
		{
			statement.setLong(1, uploadedDate);
			statement.setString(2, "%"+clipId);
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem marking clip as uploaded or not", e);
		}
	}

}
