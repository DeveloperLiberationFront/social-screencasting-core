package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.ScreenRecordingModule;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.database.LocalSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.database.RemoteToolReporter;
import edu.ncsu.lubick.localHub.forTesting.LocalHubDebugAccess;
import edu.ncsu.lubick.localHub.http.HTTPServer;
import edu.ncsu.lubick.localHub.http.WebToolReportingInterface;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.util.FileUtilities;

public class LocalHub implements  WebQueryInterface, WebToolReportingInterface {

	public static final String LOGGING_FILE_PATH = "/etc/log4j.settings";
	private static final LocalHub singletonHub;
	private static final String SCREENCASTING_PATH = "Screencasting";
	private static Logger logger;

	// Static initializer to get the logging path set up and create the hub
	static
	{
		logger = Logger.getLogger(LocalHub.class.getName());
		singletonHub = new LocalHub();
	}

	private boolean isRunning = false;
	private File monitorDirectory = null;

	private BufferedDatabaseManager databaseManager = null;
	private PostProductionHandler postProductionHandler; // = new PostProductionHandler(new File(".\\HF\\Screencasting"));

	private boolean shouldUseHTTPServer;
	private boolean shouldUseScreenRecording;
	private boolean isDebug = false;
	private ScreenRecordingModule screenRecordingModule;
	private HTTPServer httpServer;
	private UserManager userManager;
	private RemoteToolReporter remoteToolReporter;

	public static LocalHubDebugAccess startServerAndReturnDebugAccess(String monitorLocation, boolean wantHTTP, boolean wantScreenRecording)
	{
		return startServerAndReturnDebugAccess(monitorLocation, LocalSQLDatabaseFactory.DEFAULT_SQLITE_LOCATION, wantHTTP, wantScreenRecording);
	}

	public static LocalHubDebugAccess startServerAndReturnDebugAccess(String monitorLocation, String databaseLocation, boolean wantHTTP,
			boolean wantScreenRecording)
	{
		return startServer(monitorLocation, databaseLocation, wantHTTP, wantScreenRecording, true);
	}

	public static LocalHubDebugAccess startServer(String monitorLocation, String databaseLocation, boolean wantHTTP, boolean wantScreenRecording,
			boolean isDebug)
	{
		if (!singletonHub.isRunning())
		{
			singletonHub.enableHTTPServer(wantHTTP);
			singletonHub.enableScreenRecording(wantScreenRecording);
			singletonHub.setDatabaseManager(databaseLocation);
			singletonHub.setMonitorLocation(monitorLocation);
			singletonHub.isDebug = isDebug;
			singletonHub.start();
		}

		// wraps the localHub instance in a debug wrapper to limit access to
		// some methods and to grant
		// debugging access to others.
		return new LocalHubTesting(singletonHub);
	}

	public static LocalHubProcess startServerForUse(String monitorLocation)
	{
		return startServerForUse(monitorLocation, LocalSQLDatabaseFactory.DEFAULT_SQLITE_LOCATION);
	}

	public static LocalHubProcess startServerForUse(String monitorLocation, String databaseLocation)
	{
		return startServer(monitorLocation, databaseLocation, true, true, false);
	}

	// You need to call a static method to initiate this class. It is a
	// singleton with restricted access.
	private LocalHub()
	{
		logger.debug("Logging started in creation of LocalHub "+new Date());

	}

	public boolean isDebug()
	{
		return isDebug;
	}

	private void start()
	{
		if (isRunning() || this.monitorDirectory == null)
		{
			logger.info("Did not start the server because " + (isRunning() ? "it was already running" : " no monitor directory had been set."));
			return;
		}
		isRunning = true;
		
		userManager = new UserManager(new File("."));
		if (userManager.needsUserInput())
		{
			userManager.promptUserForInfo();
		}

		if (shouldUseHTTPServer)
		{
			this.httpServer = HTTPServer.startUpAnHTTPServer(this, userManager);
			logger.debug("Server started up");
		}

		File screencastingOutputFolder = new File(this.monitorDirectory, SCREENCASTING_PATH);
		if (shouldUseScreenRecording)
		{
			
			if (!(screencastingOutputFolder.exists() || screencastingOutputFolder.mkdir()))
			{
				logger.fatal("Could not setup screencast output folder");
				return;
			}
			
			this.screenRecordingModule = new ScreenRecordingModule(screencastingOutputFolder);
			screenRecordingModule.startRecording();
		}
		
		this.postProductionHandler = new PostProductionHandler(screencastingOutputFolder, userManager);
		
		this.remoteToolReporter = new RemoteToolReporter(this.databaseManager, userManager, null);

	}

	/**
	 * Takes a file directory and attempts to create
	 * 
	 * @param monitorLocation
	 */
	private void setMonitorLocation(String monitorLocation)
	{
		if (monitorLocation == null || monitorLocation.isEmpty())
		{
			if (monitorDirectory == null)
			{
				logger.fatal("Invalid input into setMonitorLocation");
				throw new RuntimeException("Could not set the monitorLocation to " + monitorLocation);
			}
			logger.error("Invalid input into setMonitorLocation.  Continuing with old directory");
			return;
		}
		File newMonitorDirectory = new File(monitorLocation);
		if (!newMonitorDirectory.exists())
		{
			if (!newMonitorDirectory.mkdir())
			{
				if (monitorDirectory == null)
				{
					logger.fatal("Could not create the monitor directory");
					throw new RuntimeException("Could not create the monitor directory " + newMonitorDirectory);
				}
				logger.error("Could not create the monitor directory " + monitorLocation + ", continuing with old directory");
				return;
			}
			// Monitor Directory has been created and now it can be set
			logger.debug("Setting Monitor Directory to " + newMonitorDirectory);
			this.monitorDirectory = newMonitorDirectory;
		}
		else if (!newMonitorDirectory.isDirectory())
		{
			if (monitorDirectory == null)
			{
				logger.fatal("Could not create the monitor directory");
				throw new RuntimeException("Could not create the monitor directory " + newMonitorDirectory);
			}
			logger.error("Could not set the monitor directory to be " + monitorLocation + ", because it is not a directory.  Continuing with old directory.");
			return;
		}
		else
		{
			// The monitorDirectory already exists, so simply set it.
			logger.debug("Setting Monitor Directory to " + newMonitorDirectory);
			this.monitorDirectory = newMonitorDirectory;
		}
	}

	private void setDatabaseManager(String databaseLocation)
	{
		this.databaseManager = BufferedDatabaseManager.createBufferedDatabasemanager(databaseLocation);
	}

	private void enableHTTPServer(boolean b)
	{
		this.shouldUseHTTPServer = b;

	}

	private void enableScreenRecording(boolean wantScreenRecording)
	{
		this.shouldUseScreenRecording = wantScreenRecording;

	}

	public boolean isRunning()
	{
		return this.isRunning;
	}



	@Deprecated
	@Override
	public List<ToolUsage> getLastNInstancesOfToolUsage(int n, String pluginName, String toolName)
	{
		return databaseManager.getLastNInstancesOfToolUsage(n, pluginName, toolName);
	}


	public void shutDown()
	{
		if (screenRecordingModule != null)
		{
			this.screenRecordingModule.stopRecording();
		}
		if (this.httpServer != null)
		{
			httpServer.shutDown();
		}
		remoteToolReporter.shutDown();
		databaseManager.shutDown();

		isRunning = false;
		logger.info("All the way shut down!");
	}


	@Override
	public List<String> getNamesOfAllPlugins()
	{
		return databaseManager.getNamesOfAllPlugins();
	}

	@Override
	public List<ToolUsage> getAllToolUsagesForPlugin(String pluginName)
	{
		return databaseManager.getAllToolUsageHistoriesForPlugin(pluginName);
	}
	
	/**
	 * A class that allows unit tests to have indirect, controlled access to the inner workings of the LocalHub. This can only be created with a static method
	 * in LocalHub
	 * 
	 * @author kjlubick
	 * 
	 */
	private static class LocalHubTesting implements LocalHubDebugAccess
	{

		private LocalHub hubToDebug;

		public LocalHubTesting(LocalHub thisHub)
		{
			hubToDebug = thisHub;
		}

		@Override
		public boolean isRunning()
		{
			return hubToDebug.isRunning();
		}


		@Override
		public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName)
		{
			return hubToDebug.getAllToolUsagesForPlugin(currentPluginName);
		}

		@Override
		public void shutDown()
		{
			hubToDebug.shutDown();

		}


		@Override
		public List<String> getAllPluginNames()
		{
			return hubToDebug.getNamesOfAllPlugins();
		}


	}

	@Override
	public void reportToolStream(ToolStream ts)	//requests coming in from the web
	{
		//TODO write unit test for this
		logger.info("ToolStream Reported from Plugin: "+ts.getAssociatedPlugin());
		logger.debug(ts.toString());
		this.databaseManager.writeToolStreamToDatabase(ts);
		for(ToolUsage tu : ts.getAsList())
		{
			try
			{
				this.postProductionHandler.extractBrowserMediaForToolUsage(tu);
			}
			catch (MediaEncodingException e)
			{
				logger.error("Problem making media for "+tu,e);
			}
		}
	}

	public static String getCurrentUserEmail()
	{
		return singletonHub.userManager.getUserEmail();
	}

	@Override
	public List<File> getBestExamplesOfTool(String pluginName, String toolName)
	{
		List<ToolUsage> usages = this.databaseManager.getLastNInstancesOfToolUsage(5, pluginName, toolName);
		
		List<File> retVal = new ArrayList<>();
		
		for (ToolUsage toolUsage : usages)
		{
			File potentialFile = new File("renderedVideos/",FileUtilities.makeFolderNameForBrowserMediaPackage(toolUsage, userManager.getUserEmail()));
			if (potentialFile.exists() && potentialFile.isDirectory())
			{
				retVal.add(potentialFile);
			}
		}
		
		return retVal;
		
	}

}
