package edu.ncsu.lubick.localHub.database;

import java.io.IOException;
import java.sql.*;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;


/**
 * EventForwawrder is a simple java service that replicates/forwards event from
 * a local data repository to remote repositories.
 * 
 * The service runs in its own thread.
 * 
 * The process first reads a properties file.  By default, this is assumed to
 * be a properties file named eventforwarder.properties in the default package
 * in the classpath. A specific file can also be established by using the 
 * "configEF" system property from the command line.  This requires the full path 
 * to be used. 
 * 
 * Example to start application with specific command file:
 * java -DconfigEF=/home/user/custom.properties edu.ncsu.lubick.localHub.database.EventForwarder
 * 
 * Properties required to be in the file:
 *  sleepTimeSeconds    How many seconds should the application sleep before checking for new events (tool usages)
 * 	sourceJDBCDriver	What JDBC driver is used to the read the local data repository
 * 	sourceJDBCURL		What is the JDBC URL used to access the local data repository
 * 
 *  destJDBCDriver		What is the list (separated by "|") of the destination JDBC locations?
 *  destJDBCURL			What is the list (separated by "|") of the destination JDBC URLs?
 *  
 *  userID				What user executed the tool usages?
 *  
 * Optional properties:
 * 
 * @author John
 * History:
 * 20140625	Slankas	Initial version feature complete.
 * 
 * Posible future enhancements:
 * - The tool could query one of the remote systems for actions to take or configurations to change
 */
public class EventForwarder extends Thread {
	private static final Logger logger = Logger.getLogger(EventForwarder.class);
	
	/** default name for the properties file to be loaded from the classpath */
	public static final String DEFAULT_PROPERTIES_FILE = "/EventForwarder.properties";
	
	/** system property name to be used to override the default config file */
	public static final String SYSTEM_CONFIG_LOCATION_NAME = "efConfig";
	
	public static final String PROPERTY_SLEEP_TIME      = "sleepTimeSeconds";
	public static final String PROPERTY_SRC_JDBC_DRIVER = "sourceJDBCDriver";
	public static final String PROPERTY_SRC_JDBC_URL    = "sourceJDBCURL";
	
	public static final String PROPERTY_DEST_JDBC_DRIVER = "destJDBCDriver";
	public static final String PROPERTY_DEST_JDBC_URL    = "destJDBCURL";
	
	public static final String PROPERTY_USER_ID = "userID";
	
	public static final String[] REQUIRED_PROPERTIES = {PROPERTY_SLEEP_TIME, PROPERTY_SRC_JDBC_DRIVER, PROPERTY_SRC_JDBC_URL,
		PROPERTY_DEST_JDBC_DRIVER, 
		PROPERTY_DEST_JDBC_URL,
		PROPERTY_USER_ID};

	private java.util.Properties _efProperties;
	
	/**
	 * Loads the properties file from the default location.  It also allows from an override location to be used.
	 * 
	 * Note: if an IOexception is thrown, the exception is caught, a warning messages output
	 *       to the logs, and the process will continue to run.  If the required properties
	 *       are not present, then this will be handled by the validation check.
	 */
	public void loadProperties() {

		_efProperties = new java.util.Properties();
		try {
			java.io.InputStream propStream = EventForwarder.class.getResourceAsStream(DEFAULT_PROPERTIES_FILE);
			if (propStream != null) {
				_efProperties.load(propStream);
				logger.info("Loaded EventForwarder properties file from default location: "+DEFAULT_PROPERTIES_FILE);  
			}
			else {
				logger.debug("EventForwarder: loadProperties, unable to locate default properties file on classpath");				
			}
			
			// now see if the configuration file should be overriden
			String configFile = System.getProperty(SYSTEM_CONFIG_LOCATION_NAME);
			if (configFile != null) {
				_efProperties.load(new java.io.FileInputStream(configFile));
				logger.info("Loaded custom EventForwarder properties from system property location at " + configFile); 
			}		
		}
		catch (IOException e) {  
			logger.error("EventForwarder: loadProperties - IOException: "+e);
		}
	}
	
	/**
	 * Validates that the required properties are in place.  If the properties are not there,
	 * then the application halts with an return code.
	 * 
	 * If load properties has not been called, then the application will halt.
	 */
	public void validateProperties() {
		java.util.ArrayList<String> missingProperties = new java.util.ArrayList<String>();
		
		if (_efProperties == null) {
			logger.fatal("EventForwarder: validateProperties - loadProperties not yet called, exiting application");
			System.exit(1);
		}
		
		for (String propName: REQUIRED_PROPERTIES) {
			if (_efProperties.getProperty(propName) == null) { missingProperties.add(propName); 	}
		}
		
		if (missingProperties.size() > 0) {
			for (String missingProperty: missingProperties) {
				logger.fatal("EventForwarder: validatedProperties, missing property - "+ missingProperty);
			}
			logger.fatal("EventForwarder: validateProperties, not all required properties present, exiting");
			System.exit(2);
		}
		
		if ( _efProperties.getProperty(PROPERTY_DEST_JDBC_DRIVER).split("\\|").length !=  _efProperties.getProperty(PROPERTY_DEST_JDBC_URL).split("\\|").length) {
			logger.fatal("EventForwarder: validateProperties, destination JDBC driver and URL properties have different couts");
			System.exit(2);			
		}
	}
	
	
	public void initializeDatabaseDrivers() throws ClassNotFoundException {
		 Class.forName(_efProperties.getProperty(PROPERTY_SRC_JDBC_DRIVER));
		 
		 String[] destinationDrivers = this.getDestinationDrivers();
		 for (String driver: destinationDrivers) {
			 Class.forName(driver);
		 }
	}
	
	public String[] getDestinationDrivers() {
		return _efProperties.getProperty(PROPERTY_DEST_JDBC_DRIVER).split("\\|");
	}
	
	public String[] getDestinationURLs() {
		return _efProperties.getProperty(PROPERTY_DEST_JDBC_URL).split("\\|");
	}

	public java.util.List<ToolUsage> getToolUsageInStaging(java.sql.Connection srcConn)
	{
		java.util.ArrayList<ToolUsage> results = new java.util.ArrayList<ToolUsage>();
		
		String sqlQuery = "SELECT  use_id, plugin_name, usage_timestamp, tool_name, " +
	                      "        tool_key_presses, class_of_tool, tool_use_duration, clip_score "+
				          "  FROM ToolUsagesStage";
		
		try {
			PreparedStatement statement = srcConn.prepareStatement(sqlQuery);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String useID = rs.getString("use_id");
				String toolName = rs.getString("tool_name");
				String pluginName = rs.getString("plugin_name");
				Date timestamp = new Date(rs.getLong("usage_timestamp"));
				String toolClass = rs.getString("class_of_tool");

				String keyPresses = rs.getString("tool_key_presses");
				int duration = rs.getInt("tool_use_duration");
				int clipScore = rs.getInt("clip_score");
					

				results.add( new ToolUsage (useID, toolName, toolClass, keyPresses, pluginName, timestamp, duration, clipScore) );
			}
		}
		catch (SQLException ex) {
			throw new DBAbstractionException(ex);
		}

		return results;
		
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
	
	public boolean insertToolUsageInDestination(ToolUsage tu, String userID, Connection destConn)	{
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
			statement.setString(3, tu.getPluginName());
			statement.setTimestamp(4, new java.sql.Timestamp( tu.getTimeStamp().getTime()));
			statement.setString(5, tu.getToolName());
			statement.setString(6, tu.getToolKeyPresses());
			statement.setString(7, tu.getToolClass());
			statement.setInt(8, tu.getDuration());
			statement.setInt(9, tu.getClipScore());
			
			logger.debug(String.format("INSERT INTO tool_usages ( use_id, userID, plugin_name, usage_timestamp, tool_name, tool_key_presses, class_of_tool, "+
				"tool_use_duration, clip_score  ) VALUES (%s,%s,%s,%s,%s,%s,%s,%d,%d)",tu.getUseID(), userID, tu.getPluginName(), tu.getTimeStamp(), 
				tu.getToolName(), tu.getToolKeyPresses(), tu.getToolClass(),tu.getDuration(), tu.getClipScore()));
	
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
	

	/**
	 * Run continuously performs these steps:
	 * log message that we are checking
	 * open source connection
     * Get stage list
     * if entries 
     *   open destination connections
     *   for each entry
     *     for each destination
     *       insert record
     *     if destination entries made (or insert failed from existing record), delete from source
     *   close destination connections
     * close source connection
     * sleep for x seconds
     * 
     * The ordering of the destination
	 */
	public void run() {
 
    	while (true) {
			logger.debug("starting cycle");
			
			java.sql.Connection srcConn = null;
			String[] destURLs = this.getDestinationURLs();
			java.sql.Connection destConn[] = new java.sql.Connection[destURLs.length];
			try {
				srcConn = DriverManager.getConnection(_efProperties.getProperty(PROPERTY_SRC_JDBC_URL));
				java.util.List<ToolUsage> usages = this.getToolUsageInStaging(srcConn);
				if (usages.size() > 0) {
					String userID = _efProperties.getProperty(PROPERTY_USER_ID);
					
		    		//  open destination connections
					for (int i=0;i < destURLs.length; i++) {
						try {
							destConn[i] = DriverManager.getConnection(destURLs[i]);
						}
						catch (SQLException e) {
							logger.warn("Unable to open dest connection to url #"+i);  // the URL has the username/password, so we can't print that in the log							
						}
					}
					
					// copy over each record to all destinations
					for (ToolUsage tu: usages) {
						int destSuccessfulCount = 0;
						for (Connection conn: destConn) {
							if (this.insertToolUsageInDestination(tu, userID, conn)) {
								destSuccessfulCount++;
							}
							else {
								if (conn != null) { 
									logger.warn("Unable to move toolUsage: "+tu); 															
								}
							}
						}
						if (destSuccessfulCount == destConn.length) {
							this.deleteToolUsageInStaging(tu.getUseID(), srcConn);
						}
					}
				}
				else {
					logger.debug("no tool useages to forward");
				}
			}
			catch (SQLException ex) {
				ex.printStackTrace();
				//TODO
			}
			finally {
				if (srcConn != null) {
					try {
						srcConn.close();
					}
					catch (SQLException ex2) {
						logger.warn("Unable to close source connection");
					}
				}
				for (Connection conn: destConn) {
					try {
						if (conn != null) { conn.close(); }
					}
					catch (SQLException ex2) {
						logger.warn("Unable to close destination connection");
					}					
				}
			}

			// now sleep before the next
    		try {
    			long sleepTime = Long.parseLong(_efProperties.getProperty(PROPERTY_SLEEP_TIME))  *1000; //need to convert seconds in property file to milliseconds
    			logger.debug("ending cycle");
    			logger.debug("sleeping time(ms): "+sleepTime);
    			Thread.sleep(sleepTime);
    		}
    		catch (Exception e) {
    			System.err.println(e);
    		}
    	}
    }

    public static void main(String args[]) {
    	BasicConfigurator.configure();
    	logger.setLevel(Level.ALL);
    	
    	EventForwarder ef = new EventForwarder();
    	
    	try {
    		ef.loadProperties();
    		ef.validateProperties();
    		
    		//System.out.println("default loaded: " + ef._efProperties);
    		//System.out.println("----------------------------------------");
    		
    		ef.initializeDatabaseDrivers();	
    		ef.start();
    		
    	}
    	catch (Exception e) {
    		System.err.println(e);
    		e.printStackTrace();
    	}
    }
}
