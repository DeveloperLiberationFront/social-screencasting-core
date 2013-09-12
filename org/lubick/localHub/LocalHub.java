package org.lubick.localHub;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.lubick.localHub.forTesting.LocalHubDebugAccess;

public class LocalHub {

	private static final String LOGGING_FILE_PATH = "./log4j.settings";
	private static LocalHub thisHub = null;
	private File monitorDirectory;
	
	private static Logger logger = Logger.getLogger(LocalHub.class.getName());
	
	//Static initializer to get the logging path set up
	static {
		PropertyConfigurator.configure(LocalHub.LOGGING_FILE_PATH);
	}

	//You need to call a static method to initiate this class.  It is a singleton with restricted access.
	private LocalHub(String monitorLocation) 
	{
		logger.debug("Logging started in creation of LocalHub");
		monitorDirectory = new File(monitorLocation);
		if (!monitorDirectory.exists())
		{
			if (!monitorDirectory.mkdir())
			{
				logger.fatal("Could not create the monitor directory");
				System.exit(1);
			}
		}
	}
	
	public static LocalHubDebugAccess startServerAndReturnDebugAccess(String monitorLocation) {
		if (thisHub == null)
		{
			thisHub = new LocalHub(monitorLocation);
			thisHub.start();
		}
		return new LocalHubTesting(thisHub);
	}

	private void start() {
		// TODO Auto-generated method stub
		
	}

}


class LocalHubTesting implements LocalHubDebugAccess 
{

	private LocalHub hubToDebug;

	public LocalHubTesting(LocalHub thisHub) {
		hubToDebug = thisHub;
	}

	@Override
	public void addLoadedFileListener(LoadedFileListener loadedFileListener) {
		// TODO Auto-generated method stub
		
	}
	
}