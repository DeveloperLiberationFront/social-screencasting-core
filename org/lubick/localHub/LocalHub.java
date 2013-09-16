package org.lubick.localHub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.lubick.localHub.forTesting.LocalHubDebugAccess;

public class LocalHub implements LoadedFileListener, ParsedFileListener {

	private static final String LOGGING_FILE_PATH = "./log4j.settings";
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

	//listeners
	private Set<LoadedFileListener> loadedFileListeners = new HashSet<>();
	private LocalHubRunnable currentRunnable = null;
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
			else
			{
				logger.error("Invalid input into setMonitorLocation.  Continuing with old directory");
				return;
			}
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
				else
				{
					logger.error("Could not create the monitor directory "+ monitorLocation+", continuing with old directory" );
					return;
				}
			}
			else 
			{
				//Monitor Directory has been created and now it can be set
				logger.debug("Setting Monitor Directory to "+ newMonitorDirectory);
				this.monitorDirectory = newMonitorDirectory;
			}
		}
		else if (!newMonitorDirectory.isDirectory()) 
		{
			if (monitorDirectory == null)
			{
				logger.fatal("Could not create the monitor directory");
				throw new RuntimeException("Could not create the monitor directory "+newMonitorDirectory);
			}
			else
			{
				logger.error("Could not set the monitor directory to be "+ monitorLocation+", because it is not a directory.  Continuing with old directory." );
				return;
			}
		}
		else 
		{
			//The monitorDirectory already exists, so simply set it.
			logger.debug("Setting Monitor Directory to "+ newMonitorDirectory);
			this.monitorDirectory = newMonitorDirectory;
		}
	}



	public static LocalHubDebugAccess startServerAndReturnDebugAccess(String monitorLocation) {
		if (!singletonHub.isRunning())
		{
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
		currentRunnable = new LocalHubRunnable(this);
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
	@Override
	public void parsedFile(ParsedFileEvent e) {
		//Just pass on the event
		for(ParsedFileListener parsedFileListener : parsedFileListeners)
		{
			parsedFileListener.parsedFile(e);
		}
	}


	
	//============End Listeners======================================================================================


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

	}

	/**
	 * This is the Runnable that operates on the background thread.
	 * 
	 * It constantly monitors the Monitor Folder and if anything is detected,
	 * the registered LoadedFileListener is called (This is going to be the local hub)
	 * @author Kevin Lubick
	 *
	 */
	private static class LocalHubRunnable implements Runnable
	{
		private Set<File> filesFromLastTime = new HashSet<>();
		private File monitorDirectory = null;
		private LoadedFileListener loadedFileListener;

		public LocalHubRunnable(LoadedFileListener listener) {
			this.loadedFileListener = listener;
		}

		@Override
		public void run() {
			if (monitorDirectory == null)
			{
				logger.error("LocalHubRunnable did not have a monitor folder. Thread terminating.");
				return;
			}

			//All of the currently tracked files should be here already courtesy of the setter


			while (true)
			{
				Set<File> newFiles = new HashSet<File>();
				//This is the monitoring code
				for (File child : this.monitorDirectory.listFiles()) {
					if (child.isDirectory())
					{
						logger.debug("Searching Plugin directory: "+child);
						for (File innerChild : child.listFiles()) 
						{
							if (!innerChild.isDirectory() && !filesFromLastTime.contains(innerChild))

							{
								logger.debug("Found new file "+innerChild);
								newFiles.add(innerChild);
							}
							else
							{
								//we only look one folder in.  Any other folders are ignored.
								logger.trace("Ignoring directory "+innerChild);
							}
						}
					}
					else if (!filesFromLastTime.contains(child))
					{
						logger.debug("Found new file "+child);
						newFiles.add(child);
					}
				}
				//All the new files have been found in this iteration
				for (File newFile : newFiles) 
				{
					conditionallyAddFileAfterContactingListener(newFile, false, filesFromLastTime);
				}

				//Sleep for a second and then do it all again
				try {
					Thread.sleep(1000);		//wake every second
				} catch (InterruptedException e) {
					LocalHub.logger.error("There was an interruption on the main thread",e);
				}
			}

		}

		/**
		 * Updates the monitorDirectory to be the passed argument.
		 * Clears out all currently tracked files and reloads everything
		 * @param monitorDirectory
		 */
		public void setMonitorFolder(File monitorDirectory) {
			if (!monitorDirectory.isDirectory())
			{
				logger.error("tried to set monitor folder to a non directory.  Ignoring.");
				return;
			}
			this.monitorDirectory = monitorDirectory;

			logger.debug("Setting monitor folder in LocalHubRunnable.  Clearing all previous tracked files");
			filesFromLastTime.clear();

			for (File child : this.monitorDirectory.listFiles()) {
				if (child.isDirectory())
				{
					filesFromLastTime.addAll(directorySearchHelper(child, true));
				}
				else 
				{
					conditionallyAddFileAfterContactingListener(child, true, filesFromLastTime);
				}
			}

			logger.debug("New tracked files are "+filesFromLastTime.toString());
		}

		/**
		 * Returns all files in this directory.  Does not return directories.
		 * @param dirToSearch
		 * @param isInitialLoading
		 * @return
		 */
		private Set<File> directorySearchHelper(File dirToSearch, boolean isInitialLoading) {
			Set<File> retVal = new HashSet<>();
			for (File child : dirToSearch.listFiles()) {
				if (!child.isDirectory())
				{
					conditionallyAddFileAfterContactingListener(child, isInitialLoading, retVal);
				}
				//else we only go one level of folder search.  That's just the implementation
			}
			return retVal;
		}

		/**
		 * Contacts the listener and asks if the file should be added to the tracking list
		 * @param thisFile
		 * @param isInitialLoading
		 * @param collectionToAddTo
		 * @return the response from the listener
		 */
		private int conditionallyAddFileAfterContactingListener(File thisFile, boolean isInitialLoading, Collection<File> collectionToAddTo) 
		{
			byte[] bytes;
			try {
				bytes = Files.readAllBytes(thisFile.toPath());
			} catch (IOException e) {
				logger.error("Error reading in file",e);
				bytes = "There was a problem reading the file".getBytes();
			}
			String fileContents = new String(bytes);
			
			int response = loadedFileListener.loadFileResponse(new LoadedFileEvent(thisFile.getName(),fileContents,isInitialLoading));

			if (response == NO_COMMENT)
			{
				logger.trace("Was given the go to track "+thisFile);
				collectionToAddTo.add(thisFile);
			}
			else
			{
				logger.debug("Was told not to track " + thisFile);
			}
			return response;

		}

	}

	
	

}