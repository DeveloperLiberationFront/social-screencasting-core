package edu.ncsu.lubick.localHub;

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

public class LocalHub implements  WebQueryInterface, WebToolReportingInterface {

	public static final String VERSION = "v2.9.8";
	public static final String LOGGING_FILE_PATH = "/etc/log4j.settings";
	public static final int MAX_TOOL_USAGES = 5;
	
	public static final String HIDDEN_PLUGIN_PREFIX = "[";
	
	private static final LocalHub singletonHub;
	private static final long HEART_BEAT_PERIOD = 60_000; //once a minute
	private static Logger logger;

	// Static initializer to get the logging path set up and create the hub
	static
	{
		logger = Logger.getLogger(LocalHub.class.getName());
		singletonHub = new LocalHub();
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
	private boolean shouldReportToolsRemotely;
	private ClipQualityManager clipQualityManager;
	private BrowserMediaPackageUploader clipUploader;
	
	private Map<String, Boolean> applicationsRecordingStatusMap = new HashMap<>();
	
	private Timer heartBeatTimer = new Timer(true);//Daemon timer, won't hold up termination
	
	private Timer pausingTimer = new Timer(true);	//Daemon timer, won't hold up termination
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
	
	public static LocalHubDebugAccess startServer(String screencastMonitorLocation, String databaseLocation, boolean wantHTTP, boolean wantScreenRecording,
			boolean wantRemoteToolReporting, boolean isDebug)
	{
		if (!LocalHub.isRunning())
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
			this.remoteToolReporter = new RemoteToolReporter(this.databaseManager, userManager);
		}
		
		startHeartbeat();
	}

	private void startHeartbeat()
	{
		TimerTask heartBeat = new TimerTask() {
			
			@Override
			public void run()
			{
				StringBuilder extraInfo = countUpScreencastsByApplication();
				extraInfo.append(VERSION);
				ToolUsage tu = new ToolUsage("heartbeat", extraInfo.toString(), ToolUsage.MENU_KEY_PRESS, "[ScreencastingHub]",
						new Date(), 1000, 0);
				reportToolUsage(tu);
			}

			private StringBuilder countUpScreencastsByApplication()
			{
				StringBuilder extraInfo = new StringBuilder();
				File f = new File(PostProductionHandler.MEDIA_OUTPUT_FOLDER);
				String[] generatedScreencastNames = f.list();
				if (generatedScreencastNames != null) {
					Map<String, Integer> nameMap = new HashMap<String, Integer>();
					//XXX Hardcoded for now
					nameMap.put("Excel", 0);
					nameMap.put("Eclipse", 0);
					nameMap.put("Gmail", 0);
					
					for(String s: generatedScreencastNames) {
						for (Entry<String, Integer> entry : nameMap.entrySet())
						{
							if (s.startsWith(entry.getKey()))
							{
								nameMap.put(entry.getKey(), entry.getValue() + 1);
							}
						}
					}
					for (Entry<String, Integer> entry : nameMap.entrySet())
					{
						extraInfo.append(entry.getKey() +" : "+entry.getValue()+',');
					}
				}
				return extraInfo;
			}
		};
		
		heartBeatTimer.schedule(heartBeat, new Date(), HEART_BEAT_PERIOD);
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

	public static boolean isRunning()
	{
		return singletonHub.isRunning;
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
	public List<String> getNamesOfAllApplications()
	{
		return databaseManager.getNamesOfAllNonHiddenApplications();
	}
	
	@Override
	public List<ToolCountStruct> getAllToolAggregateForApplication(String applicationName)
	{
		return databaseManager.getAllToolAggregateForPlugin(applicationName);
	}

	@Override
	public ToolCountStruct getToolAggregate(String applicationName, String toolName)
	{
		return databaseManager.getToolAggregate(applicationName, toolName);
	}

	@Override
	public void reportToolStream(List<ToolUsage> ts)	//requests coming in from the web (usually async)
	{
		logger.debug("waiting in line at localHub");
		synchronized (this)
		{
			logger.info("ToolStream Reported");
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
		List<String> pathsToDelete = this.databaseManager.getExcesiveClipNames();
		
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

	private void potentiallyMakeClipsFromToolStream(List<ToolUsage> ts)
	{
		
		if (ts.isEmpty() || applicationIsHidden(ts.get(0).getApplicationName())) 
		{
			logger.debug("Not making screencasts for "+ts.get(0).getApplicationName()+" because it is hidden");
			return;
		}
		
		for(ToolUsage tu : ts)
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

	public static boolean applicationIsHidden(String pluginName)
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
			return LocalHub.isRunning();
		}
	
	
		@Override
		public void shutDown()
		{
			hubToDebug.shutDown();
	
		}
	
	
		@Override
		public List<String> getAllPluginNames()
		{
			return hubToDebug.getNamesOfAllApplications();
		}
		
		@Override
		public void reportToolStream(List<ToolUsage> ts)
		{
			hubToDebug.reportToolStream(ts);
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
		//TODO If we ever need to share with other people, should we keep the clip on hand so we don't need to 
		//upload it again?
	}




	@Override
	public void updateActivity(String pluginName, boolean isActive)
	{
//		if (isActive) {
//			if (!userOverridePause) {
//				if (pausingTimerTask != null) {
//					pausingTimerTask.cancel();
//				}
//				this.screenRecordingModule.unpauseRecording();
//				this.applicationsRecordingStatusMap.put(pluginName, Boolean.TRUE);
//			}
//		} else {
//			applicationsRecordingStatusMap.put(pluginName, Boolean.FALSE);
//			
//			boolean areAnyActive = false;
//			for(Entry<String, Boolean> status: applicationsRecordingStatusMap.entrySet()) {
//				areAnyActive = areAnyActive || status.getValue();
//			}
//			
//			if (!areAnyActive && pausingTimerTask == null) {	//don't schedule the task twice
//				pausingTimerTask = new PausingTimerTask();
//				pausingTimer.schedule(pausingTimerTask , 60_000);
//			}
//		}
	}

	@Override
	public void userPause(boolean pauseButton)
	{
		userOverridePause = pauseButton;
		if (!pauseButton && singletonHub.screenRecordingModule != null)
		{
			logger.info("Pausing Recording module");
			screenRecordingModule.pauseRecording();
		}
		else if (userOverridePause)
		{
			// boolean areAnyActive = false;
			// for (Entry<String, Boolean> status : applicationsRecordingStatusMap.entrySet()) {
			// areAnyActive = areAnyActive || status.getValue();
			// }
			// if (areAnyActive) {
			screenRecordingModule.unpauseRecording();
			logger.info("Unpausing Recording module");
			// } else {
			// logger.info("Not unpausing Recording module because there is no active application.");
			// }
		}
	}
}
