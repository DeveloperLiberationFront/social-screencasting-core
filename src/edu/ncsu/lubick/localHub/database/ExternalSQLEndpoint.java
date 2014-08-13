package edu.ncsu.lubick.localHub.database;


import static edu.ncsu.lubick.localHub.database.EventForwarder.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolUsage;

public class ExternalSQLEndpoint implements ExternalToolUsageReporter {

	
	private static final Logger logger = Logger.getLogger(ExternalSQLEndpoint.class);
	public static final String EXTERNAL_SQL_STAGING_TABLE_NAME = "ExternalSQLStage";
	private Properties eventForwarderProperties;
	
	private Connection[] destConnections;
	
	public ExternalSQLEndpoint(Properties eventForwarderProperties)
	{
		this.eventForwarderProperties = eventForwarderProperties;
	}

	@Override
	public boolean shouldSend()
	{
		return true;
	}

	@Override
	public boolean reportTool(ToolUsage tu, String userID)
	{
		int sqlDestSuccessfulCount = 0;
		for (Connection conn: destConnections) {
			if (this.insertToolUsageInSQLDestination(tu, userID, conn)) {
				sqlDestSuccessfulCount++;
			}
			else {
				if (conn != null) { 
					logger.warn("Unable to move toolUsage: "+tu); 															
				}
			}
		}
		return sqlDestSuccessfulCount == destConnections.length;
	}

	@Override
	public String getStagingName()
	{
		return EXTERNAL_SQL_STAGING_TABLE_NAME;
	}

	@Override
	public void setUpForReporting()
	{
		String[] destURLs = this.getDestinationURLs();
		for (int i=0;i < destURLs.length; i++) {
			try {
				destConnections[i] = DriverManager.getConnection(destURLs[i]);
			}
			catch (SQLException e) {
				logger.warn("Unable to open dest connection to url #"+i);  // the URL has the username/password, so we can't print that in the log							
			}
		}	
	}
	
	public void initializeDatabaseDrivers() throws ClassNotFoundException {
		 String[] destinationDrivers = this.getDestinationDrivers();
		 for (String driver: destinationDrivers) {
			 Class.forName(driver);
		 }
	}
	
	public String[] getDestinationDrivers() {
		return eventForwarderProperties.getProperty(PROPERTY_DEST_JDBC_DRIVER).split("\\|");
	}
	
	public String[] getDestinationURLs() {
		return eventForwarderProperties.getProperty(PROPERTY_DEST_JDBC_URL).split("\\|");
	}
	
	public boolean deleteToolUsageInStaging(String useID, Connection srcConn)
	{
		boolean result = false;
		
		String sqlQuery = "DELETE FROM ToolUsagesStage where use_id = ?";
		
		try {
			PreparedStatement statement = srcConn.prepareStatement(sqlQuery);
			statement.setString(1, useID);
			
			int numRowsDeleted = statement.executeUpdate();
			if (numRowsDeleted == 1) { result = true; }
		}
		catch (SQLException ex) {
			throw new DBAbstractionException(ex);
		}
		return result;
	}
	
	public boolean insertToolUsageInSQLDestination(ToolUsage tu, String userID, Connection destConn)	{
		boolean result = false;
		if (destConn == null ) { return false; } 
		
		String sqlQuery =
					"INSERT INTO  tool_usages " +	
					" ( use_id, user_id, plugin_name, usage_timestamp, tool_name, " +
					"  tool_key_presses, class_of_tool, tool_use_duration,clip_score ) "+
					" VALUES (?,?,?,?,?,?,?,?,?)";
	
		try (PreparedStatement statement = destConn.prepareStatement(sqlQuery);) {
			statement.setString(1, tu.getUseID());
			statement.setString(2, userID);
			statement.setString(3, tu.getApplicationName());
			statement.setTimestamp(4, new java.sql.Timestamp( tu.getTimeStamp().getTime()));
			statement.setString(5, tu.getToolName());
			statement.setString(6, tu.getToolKeyPresses());
			statement.setString(7, tu.getToolClass());
			statement.setInt(8, tu.getDuration());
			statement.setInt(9, tu.getUsageScore());
			
			logger.debug(String.format("INSERT INTO tool_usages ( use_id, userID, plugin_name, usage_timestamp, tool_name, tool_key_presses, class_of_tool, "+
				"tool_use_duration, clip_score  ) VALUES (%s,%s,%s,%s,%s,%s,%s,%d,%d)",tu.getUseID(), userID, tu.getApplicationName(), tu.getTimeStamp(), 
				tu.getToolName(), tu.getToolKeyPresses(), tu.getToolClass(),tu.getDuration(), tu.getUsageScore()));
	
			int numRowsInserted = statement.executeUpdate();
			if (numRowsInserted == 1) { result = true; }
		}
		catch (SQLException ex) {
			if (ex.getMessage().toLowerCase().contains("unique") || ex.getMessage().toLowerCase().contains("duplicate")) {  // this works for postgres, but not tested on error messages for duplicate record
				result = true;
			}
			else {
				throw new DBAbstractionException(ex);
			}
		}
		
		return result;
	}

	@Override
	public boolean initialize()
	{
		try {
			initializeDatabaseDrivers();
		}
		catch (ClassNotFoundException e) {
			logger.fatal("Could not setup DatabaseDrivers");
			return false;
		}
		return true;
	}
	
	@Override
	public void finishReporting() {
		for (Connection conn: destConnections) {
			try {
				if (conn != null) { conn.close(); }
			}
			catch (SQLException ex2) {
				logger.warn("Unable to close destination connection", ex2);
			}					
		}
	}

}
