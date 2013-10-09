package edu.ncsu.lubick.localHub.database;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public abstract class SQLDatabase extends DBAbstraction  {

	protected abstract void executeWithNoResults(String string); 
	protected abstract ResultSet executeWithResults(String sql);
	protected abstract void cleanUpAfterQuery();

	protected void createTables() {
		createToolUsageTable();
		createRawVideoCapFiles();
	}
	
	
	private void createRawVideoCapFiles() {
		/*
		RawVideoCapFiles Schema
			CREATE TABLE IF NOT EXISTS RawVideoCapFiles (
				file_id INTEGER PRIMARY KEY AUTOINCREMENT
				file_name TEXT,
				video_start_time INTEGER, //The video's start time is only accurate to the nearest second
				duration INTEGER //This is in seconds
				
			)
		 */
		//build up the sql
		StringBuilder sqlTableQueryBuilder = new StringBuilder();
		sqlTableQueryBuilder.append("CREATE TABLE IF NOT EXISTS RawVideoCapFiles ( ");
		sqlTableQueryBuilder.append("file_id INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sqlTableQueryBuilder.append("file_name TEXT, ");
		sqlTableQueryBuilder.append("video_start_time INTEGER, ");
		sqlTableQueryBuilder.append("duration INTEGER");
		sqlTableQueryBuilder.append(") ");
	
		//execute the query
		executeWithNoResults(sqlTableQueryBuilder.toString());
		cleanUpAfterQuery();
	}
	private void createToolUsageTable() {
		/*
		ToolUsage Schema
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
		cleanUpAfterQuery();
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
		cleanUpAfterQuery();
	}
	
	
	@Override
	public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName) {
		
		List<ToolUsage> toolUsages = new ArrayList<>();
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM ToolUsages ");
		sqlQueryBuilder.append("WHERE plugin_name='");
		sqlQueryBuilder.append(currentPluginName);
		sqlQueryBuilder.append("'");
		
		try (ResultSet results = executeWithResults(sqlQueryBuilder.toString());)
		{
			//perform the query
			
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
		finally
		{
			cleanUpAfterQuery();
		}
		
		return toolUsages;
	}
	
	@Override
	public void storeVideoFile(File newVideoFile, Date videoStartTime, int durationOfClip) {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("INSERT INTO RawVideoCapFiles ( ");
		sqlQueryBuilder.append("file_name, ");
		sqlQueryBuilder.append("video_start_time, ");
		sqlQueryBuilder.append("duration ) VALUES ('");
		sqlQueryBuilder.append(newVideoFile.getAbsolutePath());
		sqlQueryBuilder.append("',");
		sqlQueryBuilder.append(videoStartTime.getTime() / 1000); //The video's start time is only accurate to the nearest second
		sqlQueryBuilder.append(",");
		sqlQueryBuilder.append(durationOfClip); //This is in seconds
		sqlQueryBuilder.append(")");
		
		executeWithNoResults(sqlQueryBuilder.toString());
		cleanUpAfterQuery();
		
	}
	
	@Override
	public ToolUsage getLastInstanceOfToolUsage(String pluginName, String toolName) {
		ToolUsage toolUsage = null;
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM ToolUsages ");
		sqlQueryBuilder.append("WHERE plugin_name='");
		sqlQueryBuilder.append(pluginName);
		sqlQueryBuilder.append("' AND tool_name='");
		sqlQueryBuilder.append(toolName);
		sqlQueryBuilder.append("' ORDER BY usage_timestamp DESC");
		
		try (ResultSet results = executeWithResults(sqlQueryBuilder.toString());)
		{
			//perform the query
			if(results.next())
			{
				Date timestamp = new Date(results.getLong("usage_timestamp"));
				String toolClass = results.getString("class_of_tool");
		
				String keyPresses = results.getString("tool_key_presses");
				int duration = results.getInt("tool_use_duration");


				toolUsage = new ToolUsage(toolName, toolClass, keyPresses, timestamp, duration);
			}			

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}
		finally
		{
			cleanUpAfterQuery();
		}
		
		return toolUsage;
	}
	
	/*
	RawVideoCapFiles Schema
		CREATE TABLE IF NOT EXISTS RawVideoCapFiles (
			file_id INTEGER PRIMARY KEY AUTOINCREMENT
			file_name TEXT,
			video_start_time INTEGER, //The video's start time is only accurate to the nearest second
			duration INTEGER //This is in seconds
			
		)
	 */
	
	public List<FileDateStructs> getVideoFilesLinkedToTimePeriod(Date timeStamp, int duration)
	{
		List<FileDateStructs> retVal = new ArrayList<>();
		
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT file_name, video_start_time FROM RawVideoCapFiles ");
		sqlQueryBuilder.append("WHERE (video_start_time<");
		sqlQueryBuilder.append(timeStamp.getTime()/1000);
		sqlQueryBuilder.append(" AND video_start_time+duration>");
		sqlQueryBuilder.append(timeStamp.getTime()/1000);
		sqlQueryBuilder.append(") OR ( video_start_time<");
		sqlQueryBuilder.append((timeStamp.getTime()+duration*2L) / 1000);
		sqlQueryBuilder.append(" AND video_start_time+duration>");
		sqlQueryBuilder.append((timeStamp.getTime()+duration*2L) / 1000);
		sqlQueryBuilder.append(")");
		
		try (ResultSet results = executeWithResults(sqlQueryBuilder.toString());)
		{
			//perform the query
			while(results.next())
			{
				Date videoTimestamp = new Date(results.getLong("video_start_time")*1000);	//multiply by 1000 to convert from seconds to millis
				File file = new File(results.getString("file_name"));

				FileDateStructs newResult = new FileDateStructs(file, videoTimestamp);

				retVal.add(newResult);
			}			

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}
		finally
		{
			cleanUpAfterQuery();
		}
		
		return retVal;
	}
	
	@Override
	public List<String> getNamesOfAllPlugins() 
	{
		String sqlQuery = "SELECT DISTINCT plugin_name FROM ToolUsages";
		
		List<String> retVal = new ArrayList<String>();
		
		try (ResultSet results = executeWithResults(sqlQuery);)
		{
			//perform the query
			while(results.next())
			{
				String plugin_name= results.getString("plugin_name");

				retVal.add(plugin_name);
			}			

		}
		catch (SQLException ex)
		{
			throw new DBAbstractionException(ex);
		}
		finally
		{
			cleanUpAfterQuery();
		}
		return retVal;
	}
	
}
