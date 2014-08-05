package edu.ncsu.lubick.localHub;

import java.awt.PopupMenu;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.ScreenRecordingModule;
import edu.ncsu.lubick.externalAPI.BrowserMediaPackageUploader;
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
import edu.ncsu.lubick.util.ToolCountStruct;

public class LocalHub implements  WebQueryInterface, WebToolReportingInterface, NotificationListener {

	public static final String LOGGING_FILE_PATH = "/etc/log4j.settings";
	public static final int MAX_TOOL_USAGES = 5;
	
	public static final String HIDDEN_PLUGIN_PREFIX = "[";
	
	private static final LocalHub singletonHub;
	private static Logger logger;

	// Static initializer to get the logging path set up and create the hub
	static
	{
		logger = Logger.getLogger(LocalHub.class.getName());
		singletonHub = new LocalHub();
		// TODO Following line added for Debugging status page's recording button. The dummy plugin 
		// lets userPause(boolean) to start the recording module even when there is no actual plugin 
		// running 		
		singletonHub.pluginsRecordingStatusMap.put("debug", true);
	}
	private boolean userOverridePause = false;
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
	private BrowserMediaPackageUploader clipUploader;
	
	private Map<String, Boolean> pluginsRecordingStatusMap = new HashMap<>();
	private Timer pausingTimer = new Timer(true);	//Daemon timer
	private TimerTask pausingTimerTask = null;

	public static LocalHubDebugAccess startTESTINGServerAndReturnDebugAccess(String screencastMonitorLocation)
	{
		return startServerAndReturnDebugAccess(screencastMonitorLocation, TestingUtils.TEST_DB_LOC, false, false, false);
	}

	private static LocalHubDebugAccess startServerAndReturnDebugAccess(String screencastMonitorLocation, String databaseLocation, boolean wantHTTP,
			boolean wantScreenRecording, boolean wantRemoteToolReporting)
	{
		return startServer(screencastMonitorLocation, databaseLocation, wantHTTP, wantScreenRecording, wantRemoteToolReporting, true);
	}
	
	public void userPause(boolean pauseButton) {
		userOverridePause = pauseButton;
		if (!pauseButton && singletonHub.screenRecordingModule != null) {
			logger.info("Pausing Recording module");
			screenRecordingModule.pauseRecording();
		} else if (pauseButton) {			
			boolean areAnyActive = false;
			for (Entry<String, Boolean> status : pluginsRecordingStatusMap
					.entrySet()) {
				areAnyActive = areAnyActive || status.getValue();
			}
			if (areAnyActive) {
				screenRecordingModule.unpauseRecording();
				logger.info("Unpausing Recording module");
			} else
				logger.error("Can't unpause Recording module because there is no active plugin.");
		}
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

		this.screenRecordingModule = new ScreenRecordingModule(this.screencastMonitorDirectory);
		if (shouldUseScreenRecording)
		{
			screenRecordingModule.startRecording();
			ScreencastManager.startManaging(this.screencastMonitorDirectory);
		}
		this.clipQualityManager = new ClipQualityManager(this.databaseManager);
		this.postProductionHandler = new PostProductionHandler(this.screencastMonitorDirectory, userManager);
		this.clipUploader = new BrowserMediaPackageUploader(userManager);
		
		if (shouldReportToolsRemotely)
		{
			this.notificationManager = new NotificationManager(this);
			this.remoteToolReporter = new RemoteToolReporter(this.databaseManager, userManager, this.notificationManager);
		}
	}

	private void setUpUserManager()
	{
		File initDirectory = new File(System.getProperty("user.dir"));
		userManager = new UserManager(initDirectory);		//this will block and prompt for use input if needed
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
		logger.debug("Remote Tool Reporting "+(wantRemoteToolReporting?"":"not")+" set up");
		this.shouldReportToolsRemotely = wantRemoteToolReporting;
		
	}

	private void enableHTTPServer(boolean wantHTTPServer)
	{
		logger.debug("Local HTTP Server "+(wantHTTPServer?"":"not")+" set up");
		this.shouldUseHTTPServer = wantHTTPServer;

	}

	private void enableScreenRecording(boolean wantScreenRecording)
	{
		logger.debug("Screen recording "+(wantScreenRecording?"":"not")+" set up");
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
	public List<ToolCountStruct> getAllToolAggregateForPlugin(String pluginName)
	{
		return databaseManager.getAllToolAggregateForPlugin(pluginName);
	}

	@Override
	public ToolCountStruct getToolAggregate(String applicationName, String toolName)
	{
		return databaseManager.getToolAggregate(applicationName, toolName);
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
	
	/**
	 * Meant for internal reporting
	 * @param tu
	 */
	private void reportToolUsage(ToolUsage tu) {
		logger.debug("Internal tool usage reported" + tu);
		this.databaseManager.writeToolUsageToDatabase(tu);
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
		if (pluginIsHidden(ts.getAssociatedPlugin())) 
		{
			logger.debug("Not making screencasts for "+ts.getAssociatedPlugin()+" because it is hidden");
			return;
		}
		
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

	private boolean pluginIsHidden(String pluginName)
	{
		return pluginName.startsWith(HIDDEN_PLUGIN_PREFIX);
	}

	public static String getCurrentUserEmail()
	{
		return singletonHub.userManager.getUserEmail();
	}

	@Override
	public List<File> getBestExamplesOfTool(String pluginName, String toolName, boolean isKeyboardHuh)
	{
		List<ToolUsage> usages = this.databaseManager.getBestNInstancesOfToolUsage(5, pluginName, toolName, isKeyboardHuh);
		
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

	private class PausingTimerTask extends TimerTask {
		@Override
		public void run()
		{
			screenRecordingModule.pauseRecording();
		}
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

	/* (non-Javadoc)
	 * @see edu.ncsu.lubick.localHub.WebQueryInterface#shareClipWithUser(java.lang.String, java.lang.String, edu.ncsu.lubick.localHub.ClipOptions)
	 */
	@Override
	public void shareClipWithUser(String clipId, ClipOptions clipOptions)
	{
		if (!this.databaseManager.isClipUploaded(clipId))
		{
			ToolUsage toolUsage = databaseManager.getToolUsageById(clipId);
			this.clipUploader.uploadToolUsage(toolUsage, clipOptions);
			this.databaseManager.setClipUploaded(clipId, true);
		}
		
		updateClipOptions(PostProductionHandler.MEDIA_OUTPUT_FOLDER + clipId, clipOptions, false);
	}

	public void setTrayIconMenu(PopupMenu pm)
	{
		this.notificationManager.setTrayIconMenu(pm);
	}

	@Override
	public void notificationReceived(String notifications)
	{
		//Log notification for study
		ToolUsage tu = new ToolUsage("Notification received", notifications, "[GUI]", "[ScreencastingHub]", new Date(), 10, 10);
		reportToolUsage(tu);
	}

	@Override
	public void notificationClickedOn(String notification)
	{
		//Log notification for study
		ToolUsage tu = new ToolUsage("Respond to notification", notification, "[GUI]", "[ScreencastingHub]", new Date(), 10, 10);
		reportToolUsage(tu);
	}


	@Override
	public void updateClipOptions(String clipId, ClipOptions options, boolean upload)
	{
		//TODO fix or remove
		//databaseManager.setStartEndFrame(folder, options.startFrame, options.endFrame);
		
//		if(databaseManager.isClipUploaded(folder) && upload)
//		{
//			clipUploader.uploadToolUsage(getToolUsageByFolder(folder), options);
//		}
	}

	@Override
	public void updateActivity(String pluginName, boolean isActive)
	{
		if (isActive) {
			if (pausingTimerTask != null) {
				pausingTimerTask.cancel();
			}
			this.screenRecordingModule.unpauseRecording();
			this.pluginsRecordingStatusMap.put(pluginName, Boolean.TRUE);
		} else {
			pluginsRecordingStatusMap.put(pluginName, Boolean.FALSE);
			
			boolean areAnyActive = false;
			for(Entry<String, Boolean> status: pluginsRecordingStatusMap.entrySet()) {
				areAnyActive = areAnyActive || status.getValue();
			}
			
			if (!areAnyActive && pausingTimerTask == null) {	//don't schedule the task twice
				pausingTimerTask = new PausingTimerTask();
				pausingTimer.schedule(pausingTimerTask , 60_000);
			}
		}
	}
}
