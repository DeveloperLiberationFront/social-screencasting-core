package org.lubick.localHub;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.lubick.localHub.ToolStream.ToolUsage;
import org.lubick.localHub.database.DBAbstraction;
import org.lubick.localHub.database.DBAbstractionFactory;
import org.lubick.localHub.database.SQLDatabaseFactory;
import org.lubick.localHub.forTesting.LocalHubDebugAccess;

public class LocalHub implements LoadedFileListener, ToolStreamFileParser {

	public static final String LOGGING_FILE_PATH = "./log4j.settings";
	private static final LocalHub singletonHub;
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
	private SimpleDateFormat sdf = new SimpleDateFormat("DDDyykkmm");
	private FileManager currentRunnable = null;
	private DBAbstraction dbAbstraction = null;

	//listeners
	private Set<LoadedFileListener> loadedFileListeners = new HashSet<>();	
	private Set<ParsedFileListener> parsedFileListeners = new HashSet<>();
	

	//You need to call a static method to initiate this class.  It is a singleton with restricted access.
	private LocalHub() {
		logger.debug("Logging started in creation of LocalHub");
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
	
	private void setDatabase(String databaseLocation) {
		this.dbAbstraction = DBAbstractionFactory.createDatabase(databaseLocation, DBAbstractionFactory.SQL_IMPLEMENTATION);
		
	}



	public static LocalHubDebugAccess startServerAndReturnDebugAccess(String monitorLocation) 
	{
		if (!singletonHub.isRunning())
		{
			singletonHub.setDatabase(SQLDatabaseFactory.DEFAULT_SQLITE_LOCATION);
			singletonHub.setMonitorLocation(monitorLocation);
			singletonHub.start();
		}
		

		return new LocalHubTesting(singletonHub);
	}



	


	private void start() {
		if (isRunning() || this.monitorDirectory == null)
		{
			logger.debug("Did not start the server because "+ (isRunning() ? "it was already running": " no monitor directory had been set."));
			return;
		}
		isRunning = true;
		currentRunnable = new FileManager(this, this);
		currentRunnable.setMonitorFolder(this.monitorDirectory);
		currentThread = new Thread(currentRunnable);
		currentThread.start();

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
	/*@Override
	public void parsedFile(ParsedFileEvent e) {
		//Just pass on the event
		for(ParsedFileListener parsedFileListener : parsedFileListeners)
		{
			parsedFileListener.parsedFile(e);
		}
	}*/


	
	//============End Listeners======================================================================================

	@Override
	public void parseFile(File fileToParse) {
		
		String fileContents = FileUtilities.readAllFromFile(fileToParse);
		ToolStream ts = ToolStream.generateFromJSON(fileContents);
		
		String fileName = fileToParse.getName();
		String pluginName = fileName.substring(0, fileName.indexOf('.'));
		String dateString = fileName.substring(fileName.indexOf('.') + 1,fileName.lastIndexOf('.'));
		
		Date associatedDate = null;
		try {
			associatedDate = sdf.parse(dateString);
		} catch (ParseException e) {
			logger.error("Trouble parsing Date "+dateString,e);
		}
		
		ParsedFileEvent event = new ParsedFileEvent(fileContents, ts, pluginName, associatedDate, fileToParse);

		for(ParsedFileListener parsedFileListener : parsedFileListeners)
		{
			parsedFileListener.parsedFile(event);
		}
	}
	
	public void shutDown() {
		currentRunnable.stop();
		isRunning = false;
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
			return hubToDebug.dbAbstraction.getAllToolUsageHistoriesForPlugin(currentPluginName);
		}

		@Override
		public void shutDown() {
			hubToDebug.shutDown();
			
		}

	}




}