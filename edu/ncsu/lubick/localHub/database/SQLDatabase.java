package edu.ncsu.lubick.localHub.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public abstract class SQLDatabase extends DBAbstraction  {

	protected abstract void executeWithNoResults(String string); 
	protected abstract ResultSet executeWithResults(String sql);
	

	protected void createTables() {
		createToolUsageTable();
	
	}
	
	
	private void createToolUsageTable() {
		/*
		Event Schema
			CREATE TABLE IF NOT EXISTS ToolUsages (
				use_id INTEGER PRIMARY KEY AUTOINCREMENT
				plugin_name TEXT,
				usage_timestamp INTEGER,
				tool_name TEXT, 
				tool_key_presses TEXT,
				class_of_tool TEXT,
				tool_use_duration INTEGER
			)
		 */
		//build up the sql
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
	
		//execute the query
		executeWithNoResults(sqlTableQueryBuilder.toString());
	}
	
	
	@Override
	public void storeToolUsage(ToolUsage tu, String associatedPlugin) {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("INSERT INTO ToolUsages ( ");
		sqlQueryBuilder.append("plugin_name, ");
		sqlQueryBuilder.append("usage_timestamp, ");
		sqlQueryBuilder.append("tool_name, ");
		sqlQueryBuilder.append("tool_key_presses, ");
		sqlQueryBuilder.append("class_of_tool, ");
		sqlQueryBuilder.append("tool_use_duration ) VALUES ('");
		sqlQueryBuilder.append(associatedPlugin);
		sqlQueryBuilder.append("',");
		sqlQueryBuilder.append(tu.getTimeStamp().getTime());
		sqlQueryBuilder.append(",'");
		sqlQueryBuilder.append(tu.getToolName()); 
		sqlQueryBuilder.append("', '");
		sqlQueryBuilder.append(tu.getToolKeyPresses());
		sqlQueryBuilder.append("', '");
		sqlQueryBuilder.append(tu.getToolClass()); 
		sqlQueryBuilder.append("', ");
		sqlQueryBuilder.append(tu.getDuration()); 
		sqlQueryBuilder.append(")");
		
		executeWithNoResults(sqlQueryBuilder.toString());
	}
	
	
	@Override
	public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName) {
		
		List<ToolUsage> toolUsages = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT * FROM ToolUsages ");
		builder.append("WHERE plugin_name='");
		builder.append(currentPluginName);
		builder.append("'");
		
		try
		{
			//perform the query
			ResultSet results = executeWithResults(builder.toString());

			while(results.next())
			{
				String toolName = results.getString("tool_name");
				Date timestamp = new Date(results.getLong("usage_timestamp"));
				String toolClass = results.getString("class_of_tool");
		
				String keyPresses = results.getString("tool_key_presses");
				int duration = results.getInt("tool_use_duration");


				toolUsages.add(new ToolUsage(toolName, toolClass, keyPresses, timestamp, duration));
			}
		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}
		
		
		return toolUsages;
	}
}
