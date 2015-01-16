package edu.ncsu.lubick.localHub.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;


/**
 * EventForwawrder is a simple java service that replicates/forwards event from
 * a local data repository to remote repositories.
 * 
 * The service is meant to be run in its own thread
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
 * 
 *  destJDBCDriver		What is the list (separated by "|") of the destination JDBC locations?
 *  destJDBCURL			What is the list (separated by "|") of the destination JDBC URLs?
 *  
 *  skylrDestAddURL		What is the URL to use when sending data to skylr?
 *  skylrDestQueryURL	What is the URL to use when querying skylr?

 * Optional properties:
 * 
 * @author John
 * History:
 * 20140625	Slankas	Initial version feature complete.
 * 
 * Posible future enhancements:
 * - The tool could query one of the remote systems for actions to take or configurations to change
 */
public class EventForwarder implements Runnable {
	private static final Logger logger = Logger.getLogger(EventForwarder.class);

	/** default name for the properties file to be loaded from the classpath */
	public static final String DEFAULT_PROPERTIES_FILE = "/etc/EventForwarder.properties";

	/** system property name to be used to override the default config file */
	public static final String SYSTEM_CONFIG_LOCATION_NAME = "efConfig";

	public static final String PROPERTY_SLEEP_TIME      = "sleepTimeSeconds";

	public static final String PROPERTY_DEST_JDBC_DRIVER = "destJDBCDriver";
	public static final String PROPERTY_DEST_JDBC_URL    = "destJDBCURL";

	public static final String PROPERTY_DEST_SKYLR_ADD_URL   = "skylrDestAddURL";
	public static final String PROPERTY_DEST_SKYLR_QUERY_URL = "skylrDestQueryURL";

	public static final String[] REQUIRED_PROPERTIES = {PROPERTY_SLEEP_TIME,
		PROPERTY_DEST_JDBC_DRIVER,	PROPERTY_DEST_JDBC_URL, PROPERTY_DEST_SKYLR_ADD_URL, PROPERTY_DEST_SKYLR_QUERY_URL};

	private Properties eventForwarderProperties;
	private BufferedDatabaseManager localDatabase;

	private UserManager userManager;

	List<ExternalToolUsageReporter> customEndPoints = new ArrayList<>();

	public EventForwarder(UserManager userManager, BufferedDatabaseManager localDatabase)
	{
		this.userManager = userManager;
		this.localDatabase = localDatabase;

		loadProperties();
		setUpEndpoints();
	}

	private void setUpEndpoints()
	{
		customEndPoints.add(new RecommendationToolReporter(this.userManager));
		customEndPoints.add(new SkylerEndpoint(eventForwarderProperties));
		//customEndPoints.add(new ExternalSQLEndpoint(eventForwarderProperties));
		
				
		for (Iterator<ExternalToolUsageReporter> iterator = customEndPoints.iterator(); iterator.hasNext();)
		{
			ExternalToolUsageReporter endpoint = (ExternalToolUsageReporter) iterator.next();
			if (!endpoint.initialize()){
				logger.error("Could not initialize "+endpoint+", so removing from list");
				iterator.remove();
			}
		}	
	}

	/**
	 * Loads the properties file from the default location.  It also allows from an override location to be used.
	 * 
	 * Note: if an IOexception is thrown, the exception is caught, a warning messages output
	 *       to the logs, and the process will continue to run.  If the required properties
	 *       are not present, then this will be handled by the validation check.
	 */
	private void loadProperties() {

		eventForwarderProperties = new java.util.Properties();
		try (InputStream propStream = EventForwarder.class.getResourceAsStream(DEFAULT_PROPERTIES_FILE);)
		{	
			if (propStream != null) {
				eventForwarderProperties.load(propStream);
				logger.info("Loaded EventForwarder properties file from default location: "+DEFAULT_PROPERTIES_FILE);  
			}
			else {
				logger.debug("EventForwarder: loadProperties, unable to locate default properties file on classpath");				
			}

			// now see if the configuration file should be overriden
			String configFile = System.getProperty(SYSTEM_CONFIG_LOCATION_NAME);
			if (configFile != null) {
				try (FileInputStream configFOS = new FileInputStream(configFile)){
					eventForwarderProperties.load(configFOS);
				}
				logger.info("Loaded custom EventForwarder properties from system property location at " + configFile); 
			}
			else if (propStream == null){
				throw new DBAbstractionException("Could not find config file in default location nor overriden location");
			}
		}
		catch (IOException e) {  
			logger.error("EventForwarder: loadProperties - IOException: "+e);
		}
		validateProperties();
	}

	/**
	 * Validates that the required properties are in place.  If the properties are not there,
	 * then the application throws a DBAbstractionException
	 * 
	 * If load properties has not been called, then the application will halt.
	 */
	private void validateProperties() {
		List<String> missingProperties = new ArrayList<String>();

		if (eventForwarderProperties == null) {
			throw new DBAbstractionException("EventForwarder: validateProperties - loadProperties not yet called, exiting application");
		}

		for (String propName: REQUIRED_PROPERTIES) {
			if (eventForwarderProperties.getProperty(propName) == null) { missingProperties.add(propName); 	}
		}

		if (missingProperties.size() > 0) {
			for (String missingProperty: missingProperties) {
				logger.fatal("EventForwarder: validatedProperties, missing property - "+ missingProperty);
			}
			throw new DBAbstractionException("EventForwarder: validateProperties, not all required properties present, exiting");
		}

		if ( eventForwarderProperties.getProperty(PROPERTY_DEST_JDBC_DRIVER).split("\\|").length !=  eventForwarderProperties.getProperty(PROPERTY_DEST_JDBC_URL).split("\\|").length) {
			throw new DBAbstractionException("EventForwarder: validateProperties, destination JDBC driver and URL properties have different couts");	
		}
	}

	@Override
	public void run() {
		
		try
		{
			Thread.sleep(30000); // The MongoDB requires that the tools exist before
			// being reported, so thidbs granular reporting needs to wait until after the general
			// reporting
		}
		catch (InterruptedException e1)
		{
			logger.debug("Interrupted",e1);
		}

		while (true) {
			logger.info("starting cycle");
			

			for (ExternalToolUsageReporter endpoint: customEndPoints) {
				logger.debug(endpoint);
				endpoint.setUpForReporting();

				List<ToolUsage> usages = localDatabase.getToolUsageInStaging(endpoint.getStagingName());
				logger.debug(usages);
				for(ToolUsage tu: usages) {
					if (endpoint.shouldSend()) 
					{
						logger.debug("Sending "+tu);
						// we no longer restrict sending the hidden tools because
						// we want to monitor things like the heartbeat is psuedorealtime(TM)
						if (endpoint.reportTool(tu, userManager.getUserEmail())){
							localDatabase.deleteToolUsageInStaging(tu, endpoint.getStagingName());
						} else {
							logger.info("Could not report tool "+tu+" to "+endpoint);
						}
					}
				}
				logger.debug("Done");
				endpoint.finishReporting();
			}

			// now sleep before the next
			try {
				long sleepTime = Long.parseLong(eventForwarderProperties.getProperty(PROPERTY_SLEEP_TIME))  * 1000; //need to convert seconds in property file to milliseconds
				logger.debug("ending cycle");
				logger.debug("sleeping time(ms): "+sleepTime);
				Thread.sleep(sleepTime);
			}
			catch (InterruptedException e) {
				logger.info("Interrupted", e);
			}
		}
	}

	@SuppressWarnings("unused")
	public static void main(String args[]) {
		TestingUtils.makeSureLoggingIsSetUp();

		EventForwarder ef = new EventForwarder(UnitTestUserManager.quickAndDirtyUser(), BufferedDatabaseManager.quickAndDirtyDatabase());

		try {
			ef.loadProperties();
			ef.validateProperties();

			logger.debug("default loaded: " + ef.eventForwarderProperties);
			logger.debug("----------------------------------------");

			Thread thread = new Thread(ef);
			thread.start();

		}
		catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}






}

interface ExternalToolUsageReporter {
	/** Intial setup, return false if the reporter cannot continue at all, e.g. drivers can't be loaded*/
	boolean initialize();
	/** Setup before reporting a batch of tool uses*/
	void setUpForReporting();
	String getStagingName();

	/** return true if ready to send another tool usage.  */
	boolean shouldSend();
	boolean reportTool(ToolUsage tu, String userid);
	void finishReporting();

}
