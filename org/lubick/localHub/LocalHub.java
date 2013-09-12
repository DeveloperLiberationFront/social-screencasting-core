package org.lubick.localHub;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.lubick.localHub.forTesting.LocalHubDebugAccess;

public class LocalHub implements LoadedFileListener{

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
	private LocalHubRunnable currentRunnable;
	
	
	
	
	
	

	//You need to call a static method to initiate this class.  It is a singleton with restricted access.
	private LocalHub() {
		logger.debug("Logging started in creation of LocalHub");
	}
	
	
	
	private void setMonitorLocation(String monitorLocation) {
		monitorDirectory = new File(monitorLocation);
		if (!monitorDirectory.exists())
		{
			if (!monitorDirectory.mkdir())
			{
				logger.fatal("Could not create the monitor directory");
				throw new RuntimeException("Could not create the monitor directory "+monitorDirectory);
			}
		}
	}

	

	public static LocalHubDebugAccess startServerAndReturnDebugAccess(String monitorLocation) {
		if (!singletonHub.isRunning())
		{
			
			singletonHub.start();
		}
		singletonHub.setMonitorLocation(monitorLocation);

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

	//Listener manipulation
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
	
}

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
		
		while (true)
		{
			for (File child : this.monitorDirectory.listFiles()) {
				if (child.isDirectory())
				{
					Set<File> recursiveFiles = directorySearchHelper(child, false);
					for (File innerChild : recursiveFiles) {
						klasdljkfaslkdfj
					}
				}
			}
			try {
				Thread.sleep(1000);		//wake every second
			} catch (InterruptedException e) {
				LocalHub.logger.info("There was an interruption on the main thread",e);
			}
		}
		
	}

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
				logger.trace("Now tracking "+child);
				filesFromLastTime.add(child);
				loadedFileListener.loadFileResponse(new LoadedFileEvent(child.getName(),"blarg",true));
				//TODO handle the int return here as specified
			}
		}
		
		logger.debug("New tracked files are "+filesFromLastTime.toString());
	}
	private Set<File> directorySearchHelper(File dirToSearch, boolean isInitialLoading) {
		Set<File> retVal = new HashSet<>();
		for (File child : dirToSearch.listFiles()) {
			if (!child.isDirectory())
			{
				logger.trace("Now tracking "+child);
				retVal.add(child);
				loadedFileListener.loadFileResponse(new LoadedFileEvent(child.getName(),"blarg",isInitialLoading));
				//TODO handle the int return here as specified
			}
			//else we only go one level of folder search.  That's just the implementation
		}
		return retVal;
	}
	
}

}