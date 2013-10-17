package edu.ncsu.lubick.localHub;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.ncsu.lubick.ScreenRecordingModule;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.database.DBAbstraction.FileDateStructs;
import edu.ncsu.lubick.localHub.database.SQLDatabaseFactory;
import edu.ncsu.lubick.localHub.forTesting.LocalHubDebugAccess;
import edu.ncsu.lubick.localHub.http.HTTPServer;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionVideoHandler;

public class LocalHub implements LoadedFileListener, ToolStreamFileParser, WebQueryInterface, ParsedFileListener {


	public static final String LOGGING_FILE_PATH = "./log4j.settings";
	private static final LocalHub singletonHub;
	private static final int SCREEN_RECORDING_VIDEO_LENGTH = 60; //60 seconds for every minivideo recorded
	private static final String SCREENCASTING_PATH = "Screencasting";
	private static Logger logger;

	//Static initializer to get the logging path set up and create the hub
	static {
		PropertyConfigurator.configure(LocalHub.LOGGING_FILE_PATH);
		logger = Logger.getLogger(LocalHub.class.getName());
		singletonHub = new LocalHub();
	}

	private Thread currentThread = null;
	private boolean isRunning = false;
	private File monitorDirectory = null;
	private SimpleDateFormat dateInMinutesToNumber = new SimpleDateFormat("DDDyykkmm");
	private SimpleDateFormat dateInSecondsToNumber = new SimpleDateFormat("DDDyykkmmss");
	private FileManager currentRunnable = null;

	private BufferedDatabaseManager databaseManager = null;
	private PostProductionVideoHandler videoPostProductionHandler;


	//listeners
	private Set<LoadedFileListener> loadedFileListeners = new HashSet<>();	
	private Set<ParsedFileListener> parsedFileListeners = new HashSet<>();

	private boolean shouldUseHTTPServer;
	private boolean shouldUseScreenRecording;
	private ScreenRecordingModule screenRecordingModule;
	private HTTPServer httpServer;


	public static LocalHubDebugAccess startServerAndReturnDebugAccess(String monitorLocation, boolean wantHTTP, boolean wantScreenRecording) 
	{
		return startServerAndReturnDebugAccess(monitorLocation, SQLDatabaseFactory.DEFAULT_SQLITE_LOCATION, wantHTTP, wantScreenRecording);
	}


	public static LocalHubDebugAccess startServerAndReturnDebugAccess(String monitorLocation, String databaseLocation, boolean wantHTTP, boolean wantScreenRecording) 
	{
		if (!singletonHub.isRunning())
		{
			singletonHub.enableHTTPServer(wantHTTP);
			singletonHub.enableScreenRecording(wantScreenRecording);
			singletonHub.setDatabaseManager(databaseLocation);
			singletonHub.setMonitorLocation(monitorLocation);
			singletonHub.start();
		}
	
		//wraps the localHub instance in a debug wrapper to limit access to some methods and to grant
		//debugging access to others.
		return new LocalHubTesting(singletonHub);
	}


	public static void startServerForUse(String monitorLocation) {
		//
		startServerForUse(monitorLocation, SQLDatabaseFactory.DEFAULT_SQLITE_LOCATION);
	}


	public static void startServerForUse(String monitorLocation, String databaseLocation) {
		startServerAndReturnDebugAccess(monitorLocation, databaseLocation, true, true);
	}


	//You need to call a static method to initiate this class.  It is a singleton with restricted access.
	private LocalHub() {
		logger.debug("Logging started in creation of LocalHub");

		this.addLoadedFileListener(new VideoFileMonitor());
	}


	private void start() {
		if (isRunning() || this.monitorDirectory == null)
		{
			logger.debug("Did not start the server because "+ (isRunning() ? "it was already running": " no monitor directory had been set."));
			return;
		}
		isRunning = true;
		currentRunnable = new FileManager(this, this);
		currentRunnable.setMonitorFolderAndUpdateTrackedFiles(this.monitorDirectory);
		currentThread = new Thread(currentRunnable);
		currentThread.start();
	
		if (shouldUseHTTPServer)
		{
			this.httpServer = HTTPServer.startUpAnHTTPServer(this);
			logger.debug("Server started up");
		}
		
		if (shouldUseScreenRecording)
		{
			this.screenRecordingModule = new ScreenRecordingModule(new File(this.monitorDirectory,SCREENCASTING_PATH));
			screenRecordingModule.startRecording();
		}
	
	}


	/**
	 * Takes a file directory and attempts to create
	 * @param monitorLocation
	 */
	private void setMonitorLocation(String monitorLocation) 
	{
		if (monitorLocation == null || monitorLocation.isEmpty())
		{
			if (monitorDirectory == null)
			{
				logger.fatal("Invalid input into setMonitorLocation");
				throw new RuntimeException("Could not set the monitorLocation to "+monitorLocation);
			}
			logger.error("Invalid input into setMonitorLocation.  Continuing with old directory");
			return;
		}
		File newMonitorDirectory = new File(monitorLocation);
		if (!newMonitorDirectory.exists())
		{
			if (!newMonitorDirectory.mkdir() )
			{
				if (monitorDirectory == null)
				{
					logger.fatal("Could not create the monitor directory");
					throw new RuntimeException("Could not create the monitor directory "+newMonitorDirectory);
				}
				logger.error("Could not create the monitor directory "+ monitorLocation+", continuing with old directory" );
				return;
			}
			//Monitor Directory has been created and now it can be set
			logger.debug("Setting Monitor Directory to "+ newMonitorDirectory);
			this.monitorDirectory = newMonitorDirectory;
		}
		else if (!newMonitorDirectory.isDirectory()) 
		{
			if (monitorDirectory == null)
			{
				logger.fatal("Could not create the monitor directory");
				throw new RuntimeException("Could not create the monitor directory "+newMonitorDirectory);
			}
			logger.error("Could not set the monitor directory to be "+ monitorLocation+", because it is not a directory.  Continuing with old directory." );
			return;
		}
		else 
		{
			//The monitorDirectory already exists, so simply set it.
			logger.debug("Setting Monitor Directory to "+ newMonitorDirectory);
			this.monitorDirectory = newMonitorDirectory;
		}
	}

	private void setDatabaseManager(String databaseLocation) {
		this.databaseManager = BufferedDatabaseManager.createBufferedDatabasemanager(databaseLocation);
	}



	private void enableHTTPServer(boolean b) {
		this.shouldUseHTTPServer = b;

	}


	private void enableScreenRecording(boolean wantScreenRecording) {
		this.shouldUseScreenRecording = wantScreenRecording;
		
	}


	public boolean isRunning() {
		return this.isRunning;
	}

	//Listener manipulation======================================================================================
	public void addLoadedFileListener(LoadedFileListener loadedFileListener) {
		loadedFileListeners.add(loadedFileListener);
	}
	public void removeLoadedFileListener(LoadedFileListener loadedFileListener) {
		loadedFileListeners.remove(loadedFileListener);
	}
	@Override
	public int loadFileResponse(LoadedFileEvent e) {
		int retVal = LoadedFileListener.NO_COMMENT;
		for(LoadedFileListener lfl : loadedFileListeners)
		{
			int response = lfl.loadFileResponse(e);
			//return the most extreme value
			if (response > retVal)
			{
				retVal = response;
			}
		}
		return retVal;
	}

	public void addParsedFileListener(ParsedFileListener parsedFileListener) {
		parsedFileListeners.add(parsedFileListener);

	}
	public void removeParsedFileListener(ParsedFileListener parsedFileListener) {
		parsedFileListeners.remove(parsedFileListener);
	}
	@Override
	public void parsedFile(ParsedFileEvent e) {
		//Just pass on the event
		for(ParsedFileListener parsedFileListener : parsedFileListeners)
		{
			parsedFileListener.parsedFile(e);
		}
	}



	//============End Listeners======================================================================================

	@Override
	public void parseFile(File fileToParse) {
		logger.debug("parsing file "+fileToParse);
		String fileContents = FileUtilities.readAllFromFile(fileToParse);
		ToolStream ts = ToolStream.generateFromJSON(fileContents);

		//Expecting name convention
		//PLUGIN_NAME.ENCODEDEDATE.log
		String fileName = fileToParse.getName();
		String pluginName = fileName.substring(0, fileName.indexOf('.'));

		Date associatedDate;
		try {
			associatedDate = extractStartTime(fileName, this.dateInMinutesToNumber);
		} 
		catch (ImproperlyEncodedDateException e) {
			logger.error("Problem parsing time info from file" + fileToParse + "  Skipping...", e);
			return;
		}

		ts.setTimeStamp(associatedDate);
		ts.setAssociatedPlugin(pluginName);

		ParsedFileEvent event = new ParsedFileEvent(fileContents, ts, pluginName, associatedDate, fileToParse);

		for(ParsedFileListener parsedFileListener : parsedFileListeners)
		{
			parsedFileListener.parsedFile(event);
		}

		databaseManager.writeToolStreamToDatabase(ts);

		if (!fileToParse.delete())
		{
			logger.info("Could not delete toolstream file "+fileToParse+" but, continuing anyway.");
		}
	}

	@Override
	public File extractVideoForLastUsageOfTool(String pluginName, String toolName) 
	{
		ToolUsage lastToolUsage = databaseManager.getLastInstanceOfToolUsage(pluginName,toolName);
		videoPostProductionHandler = new PostProductionVideoHandler();

		List<FileDateStructs> filesToload = databaseManager.getVideoFilesLinkedToTimePeriod(lastToolUsage.getTimeStamp(),lastToolUsage.getDuration());

		
		logger.debug("Loading files "+filesToload);
		if (filesToload == null || filesToload.size() == 0)
		{
			return null;
		}
		videoPostProductionHandler.loadFile(filesToload.get(0).file);
		videoPostProductionHandler.setCurrentFileStartTime(filesToload.get(0).startTime);

		for(int i = 1;i<filesToload.size();i++)
		{
			videoPostProductionHandler.enqueueOverLoadFile(filesToload.get(i).file,filesToload.get(i).startTime);
		}

		return videoPostProductionHandler.extractVideoForToolUsage(lastToolUsage);
	}


	public void shutDown() {
		if (screenRecordingModule != null)
		{
			this.screenRecordingModule.stopRecording();
		}
		if (this.httpServer != null)
		{
			httpServer.shutDown();
		}
		currentRunnable.stop();
		databaseManager.shutDown();
		
		isRunning = false;
	}

	public void addVideoFileToDatabase(String fileName) {
		File newVideoFile = new File(fileName);
		Date videoStartTime;
		try {
			videoStartTime = extractStartTime(newVideoFile.getName(), dateInSecondsToNumber);
		} catch (ImproperlyEncodedDateException e) {
			logger.error("Problem with video "+fileName+", Skipping it",e);
			return;
		}
		this.databaseManager.addVideoFile(newVideoFile, videoStartTime, LocalHub.SCREEN_RECORDING_VIDEO_LENGTH);

	}

	//Expecting name convention
	//screencast.ENCODEDDATE.cap
	//OR
	//PLUGINNAME.ENCODEDDATE.log
	private Date extractStartTime(String fileName, SimpleDateFormat formatter) throws ImproperlyEncodedDateException {
		if (fileName.indexOf('.') < 0 || fileName.lastIndexOf('.') == fileName.indexOf('.'))
		{
			throw new ImproperlyEncodedDateException("Improperly formatted file name:  Should be like PLUGINNAME.ENCODEDDATE.log or screencast.ENCODEDDATE.cap, was "+fileName);
		}
		String dateString = fileName.substring(fileName.indexOf('.') + 1,fileName.lastIndexOf('.'));

		Date associatedDate = null;
		try {
			associatedDate = formatter.parse(dateString);
		} 
		catch (ParseException e) {
			throw new ImproperlyEncodedDateException("Trouble parsing Date "+dateString, e);
		}
		return associatedDate;
	}


	public List<String> getNamesOfAllPlugins() {
		return databaseManager.getNamesOfAllPlugins();
	}


	public List<ToolUsage> getAllToolUsagesForPlugin(String pluginName) {
		return databaseManager.getAllToolUsageHistoriesForPlugin(pluginName);
	}

	private class VideoFileMonitor implements LoadedFileListener 
	{
	
		@Override
		public int loadFileResponse(LoadedFileEvent e) {
			if (e.getFileName().endsWith(PostProductionVideoHandler.EXPECTED_FILE_EXTENSION))
			{
				if (!e.wasInitialReadIn())
				{
					logger.info("Found ScreenCapFile "+e.getFileName());
					addVideoFileToDatabase(e.getFullFileName());
				}
				return LoadedFileListener.DONT_PARSE;
			}
	
			return LoadedFileListener.NO_COMMENT;
		}
	
	
	}

	/**
	 * A class that allows unit tests to have indirect, controlled access to the 
	 * inner workings of the LocalHub.  This can only be created with a static method in LocalHub
	 * 
	 * @author kjlubick
	 *
	 */
	private static class LocalHubTesting implements LocalHubDebugAccess 
	{
	
		private LocalHub hubToDebug;
	
		public LocalHubTesting(LocalHub thisHub) {
			hubToDebug = thisHub;
		}
	
		@Override
		public void addLoadedFileListener(LoadedFileListener loadedFileListener) {
			hubToDebug.addLoadedFileListener(loadedFileListener);
		}
	
		@Override
		public boolean isRunning() {
			return hubToDebug.isRunning();
		}
	
		@Override
		public void removeLoadedFileListener(LoadedFileListener lflToRemove) {
			this.hubToDebug.removeLoadedFileListener(lflToRemove);
		}
	
		@Override
		public void addParsedFileListener(ParsedFileListener parsedFileListener) {
			hubToDebug.addParsedFileListener(parsedFileListener);
	
		}
	
		@Override
		public void removeParsedFileListener(ParsedFileListener parsedFileListener) {
			hubToDebug.removeParsedFileListener(parsedFileListener);
	
		}
	
		@Override
		public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName) 
		{
			return hubToDebug.getAllToolUsagesForPlugin(currentPluginName);
		}
	
		@Override
		public void shutDown() {
			hubToDebug.shutDown();
	
		}
	
		@Override
		public File extractVideoForLastUsageOfTool(String pluginName, String toolName) {
			return hubToDebug.extractVideoForLastUsageOfTool(pluginName,toolName);
		}

		@Override
		public List<String> getAllPluginNames() {
			return hubToDebug.getNamesOfAllPlugins();
		}
	
	}






}