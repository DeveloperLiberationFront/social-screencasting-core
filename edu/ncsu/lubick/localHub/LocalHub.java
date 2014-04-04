package edu.ncsu.lubick.localHub;

import java.awt.PopupMenu;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.ScreenRecordingModule;
import edu.ncsu.lubick.externalAPI.BrowserMediaPackageSharer;
import edu.ncsu.lubick.externalAPI.BrowserMediaPackageUploader;
import edu.ncsu.lubick.externalAPI.ExternalClipRequester;
import edu.ncsu.lubick.externalAPI.RemoteToolReporter;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.database.BufferedDatabaseManager;
import edu.ncsu.lubick.localHub.database.LocalSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.forTesting.LocalHubDebugAccess;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.http.HTTPServer;
import edu.ncsu.lubick.localHub.http.WebToolReportingInterface;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.util.FileUtilities;

public class LocalHub implements  WebQueryInterface, WebToolReportingInterface {

	public static final String LOGGING_FILE_PATH = "/etc/log4j.settings";
	public static final int MAX_TOOL_USAGES = 5;
	
	private static final LocalHub singletonHub;
	private static Logger logger;

	// Static initializer to get the logging path set up and create the hub
	static
	{
		logger = Logger.getLogger(LocalHub.class.getName());
		singletonHub = new LocalHub();
	}

	private boolean isRunning = false;
	private File screencastMonitorDirectory = null;

	private BufferedDatabaseManager databaseManager = null;
	private PostProductionHandler postProductionHandler; 
	
	private boolean shouldUseHTTPServer;
	private boolean shouldUseScreenRecording;
	private boolean isDebug = false;
	private ScreenRecordingModule screenRecordingModule;
	private HTTPServer httpServer;
	private UserManager userManager;
	private RemoteToolReporter remoteToolReporter;
	private NotificationManager notificationManager;
	private boolean shouldReportToolsRemotely;
	private ClipQualityManager clipQualityManager;
	private BrowserMediaPackageSharer clipSharingManager;
	private BrowserMediaPackageUploader clipUploader;
	private ExternalClipRequester clipShareRequester;

	public static LocalHubDebugAccess startTESTINGServerAndReturnDebugAccess(String screencastMonitorLocation)
	{
		return startServerAndReturnDebugAccess(screencastMonitorLocation, TestingUtils.TEST_DB_LOC, false, false, false);
	}

	private static LocalHubDebugAccess startServerAndReturnDebugAccess(String screencastMonitorLocation, String databaseLocation, boolean wantHTTP,
			boolean wantScreenRecording, boolean wantRemoteToolReporting)
	{
		return startServer(screencastMonitorLocation, databaseLocation, wantHTTP, wantScreenRecording, wantRemoteToolReporting, true);
	}

	public static LocalHubDebugAccess startServer(String screencastMonitorLocation, String databaseLocation, boolean wantHTTP, boolean wantScreenRecording,
			boolean wantRemoteToolReporting, boolean isDebug)
	{
		if (!singletonHub.isRunning())
		{
			singletonHub.isDebug = isDebug;
			singletonHub.enableHTTPServer(wantHTTP);
			singletonHub.enableScreenRecording(wantScreenRecording);
			singletonHub.enableRemoteToolReporting(wantRemoteToolReporting);
			singletonHub.setUpUserManager();
			singletonHub.setDatabaseManager(databaseLocation);
			singletonHub.setScreencastMonitorLocation(screencastMonitorLocation);
			singletonHub.start();
		}

		// wraps the localHub instance in a debug wrapper to limit access to
		// some methods and to grant
		// debugging access to others.
		return new LocalHubTesting(singletonHub);
	}

	public static LocalHubProcess startServerForUse(String screencastMonitorLocation)
	{
		return startServerForUse(screencastMonitorLocation, LocalSQLDatabaseFactory.DEFAULT_SQLITE_LOCATION);
	}

	public static LocalHubProcess startServerForUse(String screencastMonitorLocation, String databaseLocation)
	{
		return startServer(screencastMonitorLocation, databaseLocation, true, true, true, false);
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
		if (isRunning() || this.screencastMonitorDirectory == null)
		{
			logger.info("Did not start the server because " + (isRunning() ? "it was already running" : " no monitor directory had been set."));
			return;
		}
		isRunning = true;
		
		if (shouldUseHTTPServer)
		{
			this.httpServer = HTTPServer.startUpAnHTTPServer(this, userManager);
			logger.debug("Server started up");
		}

		if (shouldUseScreenRecording)
		{
			this.screenRecordingModule = new ScreenRecordingModule(this.screencastMonitorDirectory);
			screenRecordingModule.startRecording();
			ScreencastManager.startManaging(this.screencastMonitorDirectory);
		}
		this.clipQualityManager = new ClipQualityManager(this.databaseManager);
		this.postProductionHandler = new PostProductionHandler(this.screencastMonitorDirectory, userManager);
		this.clipSharingManager = new BrowserMediaPackageSharer(userManager);
		this.clipUploader = new BrowserMediaPackageUploader(userManager);
		this.clipShareRequester = new ExternalClipRequester(userManager);
		
		if (shouldReportToolsRemotely)
		{
			this.notificationManager = new NotificationManager();
			this.remoteToolReporter = new RemoteToolReporter(this.databaseManager, userManager, this.notificationManager);
		}
	}

	private void setUpUserManager()
	{
		userManager = new UserManager(new File("."));		//this will block and prompt for use input if needed
	}

	/**
	 * Takes a file directory and attempts to create
	 * 
	 * @param screencastMonitorLocation
	 */
	private void setScreencastMonitorLocation(String screencastMonitorLocation)
	{
		if (screencastMonitorLocation == null || screencastMonitorLocation.isEmpty())
		{
			if (screencastMonitorDirectory == null)
			{
				logger.fatal("Invalid input into setMonitorLocation");
				throw new RuntimeException("Could not set the monitorLocation to " + screencastMonitorLocation);
			}
			logger.error("Invalid input into setMonitorLocation.  Continuing with old directory");
			return;
		}
		File newMonitorDirectory = new File(screencastMonitorLocation);
		if (!newMonitorDirectory.exists())
		{
			if (!newMonitorDirectory.mkdirs())
			{
				if (screencastMonitorDirectory == null)
				{
					logger.fatal("Could not create the monitor directory");
					throw new RuntimeException("Could not create the monitor directory " + newMonitorDirectory);
				}
				logger.error("Could not create the monitor directory " + screencastMonitorLocation + ", continuing with old directory");
				return;
			}
			// Monitor Directory has been created and now it can be set
			logger.debug("Setting Monitor Directory to " + newMonitorDirectory);
			this.screencastMonitorDirectory = newMonitorDirectory;
		}
		else if (!newMonitorDirectory.isDirectory())
		{
			if (screencastMonitorDirectory == null)
			{
				logger.fatal("Could not create the monitor directory");
				throw new RuntimeException("Could not create the monitor directory " + newMonitorDirectory);
			}
			logger.error("Could not set the monitor directory to be " + screencastMonitorLocation + ", because it is not a directory.  Continuing with old directory.");
			return;
		}
		else
		{
			// The monitorDirectory already exists, so simply set it.
			logger.debug("Setting Monitor Directory to " + newMonitorDirectory);
			this.screencastMonitorDirectory = newMonitorDirectory;
		}
	}

	private void setDatabaseManager(String databaseLocation)
	{
		this.databaseManager = BufferedDatabaseManager.createBufferedDatabasemanager(databaseLocation, this.userManager, this.isDebug);
	}

	private void enableRemoteToolReporting(boolean wantRemoteToolReporting)
	{
		this.shouldReportToolsRemotely = wantRemoteToolReporting;
		
	}

	private void enableHTTPServer(boolean wantHTTPServer)
	{
		this.shouldUseHTTPServer = wantHTTPServer;

	}

	private void enableScreenRecording(boolean wantScreenRecording)
	{
		this.shouldUseScreenRecording = wantScreenRecording;

	}

	public boolean isRunning()
	{
		return this.isRunning;
	}


	public void shutDown()
	{
		if (screenRecordingModule != null)
		{
			this.screenRecordingModule.stopRecording();
			ScreencastManager.stopManaging();
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
		return databaseManager.getNamesOfAllNonHiddenPlugins();
	}

	@Override
	public List<ToolUsage> getAllToolUsagesForPlugin(String pluginName)
	{
		return databaseManager.getAllToolUsageHistoriesForPlugin(pluginName);
	}
	
	@Override
	public void reportToolStream(ToolStream ts)	//requests coming in from the web (usually async)
	{
		logger.debug("waiting in line at localHub");
		synchronized (this)
		{
			logger.info("ToolStream Reported from Plugin: "+ts.getAssociatedPlugin());
			logger.debug(ts.toString());
			
			//report toolStreams
			this.databaseManager.writeToolStreamToDatabase(ts);
			
			potentiallyMakeClipsFromToolStream(ts);
			
			cleanUpObsoleteClips();
		}	
	}

	private void cleanUpObsoleteClips()
	{
		// Query database for clips made and update the database with those that don't exist
		List<String> pathsToDelete = this.databaseManager.getExcesiveTools();
		
		for (String path: pathsToDelete)
		{
			File clipToDelete = new File(path);
			if (clipToDelete.exists()) {
				if (TestingUtils.clearOutDirectory(clipToDelete) && clipToDelete.delete())
				{
					logger.info("Deleted clip "+path);
				}
				else
				{
					logger.error("Could not auto-delete clip - "+path+", maybe it doesn't exist?");
				}
			}
			this.databaseManager.reportMediaDeletedForToolUsage(path);
		}
	}

	private void potentiallyMakeClipsFromToolStream(ToolStream ts)
	{
		for(ToolUsage tu : ts.getAsList())
		{
			if (clipQualityManager.shouldMakeClipForUsage(tu))
			{	
				if (shouldUseScreenRecording || isDebug)
				{
					logger.debug("Going to make clip for "+tu.getToolName());
					try
					{
						this.postProductionHandler.extractBrowserMediaForToolUsage(tu);
						String clipPath = FileUtilities.makeLocalFolderNameForBrowserMediaPackage(tu, getCurrentUserEmail());
						this.databaseManager.reportMediaMadeForToolUsage(clipPath, tu);
					}
					catch (MediaEncodingException e)
					{
						logger.error("Problem making media for "+tu,e);
					}
				}
				else {
					logger.debug("Not making clip from "+tu+" because screencasting is turned off");
				}
			}
			else {
				logger.debug("Not making clip from "+tu+" because its score isn't high enough");
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
		List<ToolUsage> usages = this.databaseManager.getBestNInstancesOfToolUsage(5, pluginName, toolName);
		
		List<File> retVal = new ArrayList<>();
		
		for (ToolUsage toolUsage : usages)
		{
			File potentialFile = new File(FileUtilities.makeLocalFolderNameForBrowserMediaPackage(toolUsage, userManager.getUserEmail()));
			if (potentialFile.exists() && potentialFile.isDirectory())
			{
				retVal.add(potentialFile);
			}
			else {
				logger.error("Clip does not exist for "+toolUsage+", but it should...");
			}
		}
		logger.info(retVal+" are the best examples for "+toolName);
		return retVal;
		
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
		
		@Override
		public void reportToolStream(ToolStream ts)
		{
			hubToDebug.reportToolStream(ts);
		}

		@Override
		public void setTrayIconMenu(PopupMenu pm)
		{
			hubToDebug.setTrayIconMenu(pm);
		}
	

	}

	@Override
	public void shareClipWithUser(String clipId, String recipient)
	{
		if (!this.databaseManager.isClipUploaded(clipId))
		{
			ToolUsage toolUsage = databaseManager.getToolUsageById(clipId);
			this.clipUploader.uploadToolUsage(toolUsage);
			this.databaseManager.setClipUploaded(clipId, true);
		}
		this.clipSharingManager.shareClipWithUser(clipId, recipient);
	}

	public void setTrayIconMenu(PopupMenu pm)
	{
		this.notificationManager.setTrayIconMenu(pm);
	}

	@Override
	public void requestClipsFromUser(String owner, String pluginName, String toolName)
	{
		this.clipShareRequester.requestClipsFromUser(owner, pluginName, toolName);
	}
}
