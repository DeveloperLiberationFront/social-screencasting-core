package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

/**
 * This is the Runnable that operates on the background thread.
 * 
 * It constantly monitors the Monitor Folder and if anything is detected, the registered LoadedFileListener is called (This is going to be the local hub)
 * 
 * @author Kevin Lubick
 * 
 */
public class FileManager implements Runnable {

	private static final int TIME_BETWEEN_SEARCHES = 5000;
	private Set<File> filesFromLastTime = new HashSet<>();
	private Set<File> filesToIgnore = new HashSet<>();
	/**
	 * Maps a string, the name of a plugin, to the queue of as of yet unparsed files related to that plugin
	 */
	private Map<String, Queue<File>> unparsedFiles = new HashMap<>();

	private File monitorDirectory = null;
	private LoadedFileListener loadedFileListener = null;
	private ToolStreamFileParser fileParser = null;
	private boolean isRunning;

	private static Logger logger = Logger.getLogger(FileManager.class.getName());

	public FileManager(LoadedFileListener loadedFileListener, ToolStreamFileParser fileParser)
	{
		this.loadedFileListener = loadedFileListener;
		this.fileParser = fileParser;
	}

	@Override
	public void run()
	{
		if (monitorDirectory == null)
		{
			logger.error("LocalHubRunnable did not have a monitor folder. Thread terminating.");
			return;
		}
		unparsedFiles.clear();

		// All of the currently tracked files should be here already courtesy of
		// the setter
		isRunning = true;

		while (isRunning)
		{
			Set<File> newFiles = findNewFilesInMonitorDirectory();

			weedOutUnwantedFiles(newFiles);

			conditionallyParseNewFiles(newFiles, false);

			// Sleep for a second and then do it all again
			try
			{
				Thread.sleep(TIME_BETWEEN_SEARCHES); // wake every second
			}
			catch (InterruptedException e)
			{
				logger.error("There was an interruption on the main thread", e);
			}
		}

	}

	/**
	 * Updates the monitorDirectory to be the passed argument. Clears out all currently tracked files and reloads everything
	 * 
	 * @param monitorDirectory
	 */
	public void setMonitorFolderAndUpdateTrackedFiles(File monitorDirectory)
	{
		if (!monitorDirectory.isDirectory())
		{
			logger.error("tried to set monitor folder to a non directory.  Ignoring.");
			return;
		}
		this.monitorDirectory = monitorDirectory;

		logger.debug("Setting monitor folder in LocalHubRunnable.  Clearing all previous tracked files");
		filesFromLastTime.clear();

		for (File child : this.monitorDirectory.listFiles())
		{
			if (child.isDirectory())
			{
				filesFromLastTime.addAll(returnAllApprovedFilesInThisDirectory(child, true));
			}
			else
			{
				conditionallyAddFileToCollectionAfterContactingListener(child, true, filesFromLastTime);
			}
		}
		logger.debug("New tracked files are " + filesFromLastTime.toString() + " parsing them");

		conditionallyParseNewFiles(filesFromLastTime, true);

	}

	public void stop()
	{
		isRunning = false;

	}

	private void weedOutUnwantedFiles(Set<File> newFiles)
	{
		for (Iterator<File> iterator = newFiles.iterator(); iterator.hasNext();)
		{
			File file = (File) iterator.next();

			int result = conditionallyAddFileToCollectionAfterContactingListener(file, false, filesFromLastTime);
			if (result == LoadedFileListener.DONT_PARSE)
			{
				iterator.remove();
			}
		}
	}

	private void conditionallyParseNewFiles(Set<File> newFiles, boolean shouldForceParsing)
	{
		for (File newFile : newFiles)
		{
			parseOrQueueFile(newFile, shouldForceParsing);
		}
	}

	private Set<File> findNewFilesInMonitorDirectory()
	{
		Set<File> newFiles = new HashSet<File>();
		// This is the monitoring code
		for (File child : this.monitorDirectory.listFiles())
		{
			if (child.isDirectory())
			{
				searchChildDirectoryForNewFiles(child, newFiles);
			}
			else if (!filesFromLastTime.contains(child) && !filesToIgnore.contains(child))
			{
				logger.debug("Found new file " + child);
				newFiles.add(child);
			}
		}
		return newFiles;
	}

	private void searchChildDirectoryForNewFiles(File directoryToSearch, Set<File> filesFoundSoFar)
	{
		logger.debug("Searching Plugin directory: " + directoryToSearch);
		for (File innerChild : directoryToSearch.listFiles())
		{
			if (!innerChild.isDirectory() && !filesFromLastTime.contains(innerChild) && !filesToIgnore.contains(innerChild))

			{
				logger.debug("Found new file " + innerChild);
				filesFoundSoFar.add(innerChild);
			}
			else
			{
				// we only look one folder in. Any other folders are ignored.
				logger.trace("Ignoring directory " + innerChild);
			}
		}
	}

	/**
	 * Takes a file, parses off the name and the time and begins to parse any files older than it
	 * 
	 * @param shouldForceParsing
	 * 
	 * @param newFile
	 */
	private void parseOrQueueFile(File file, boolean shouldForceParsing)
	{
		if (file == null)
		{
			return;
		}

		String fileName = file.getName();
		int splitPointForPluginName = fileName.indexOf('.');
		if (splitPointForPluginName == -1)
		{
			logger.info("File " + file + " can be ignored.  Improperly formated.");
			return;
		}

		String pluginName = fileName.substring(0, splitPointForPluginName);
		logger.debug(file + " was seen to belong to the plugin " + pluginName);

		Queue<File> filesToParse = unparsedFiles.get(pluginName);
		if (filesToParse == null)
		{
			logger.trace("Created queue for the plugin " + pluginName + " with file " + file);
			Queue<File> newQueue = new LinkedList<File>();
			newQueue.offer(file);
			unparsedFiles.put(pluginName, newQueue);
		}
		else
		{
			logger.debug("Adding file " + file + " to the queue");
			filesToParse.offer(file);
			// If this is the only file we haven't parsed yet, it may not be
			// fully written yet, so hold off
			if (filesToParse.size() > 1)
			{
				logger.debug("Parsing file " + filesToParse.peek());
				fileParser.parseFile(filesToParse.poll());
			}
		}

		if (shouldForceParsing)
		{
			filesToParse = unparsedFiles.get(pluginName);
			while (filesToParse.size() != 0)
			{
				logger.debug("Parsing file " + filesToParse.peek());
				fileParser.parseFile(filesToParse.poll());
			}
		}
	}

	/**
	 * Returns all files in this directory. Does not return directories.
	 * 
	 * @param dirToSearch
	 * @param isInitialLoading
	 * @return
	 */
	private Set<File> returnAllApprovedFilesInThisDirectory(File dirToSearch, boolean isInitialLoading)
	{
		Set<File> retVal = new HashSet<>();
		for (File child : dirToSearch.listFiles())
		{
			if (!child.isDirectory())
			{
				conditionallyAddFileToCollectionAfterContactingListener(child, isInitialLoading, retVal);
			}
			// else we only go one level of folder search. That's just the
			// implementation
		}
		return retVal;
	}

	/**
	 * Contacts the listener and asks if the file should be added to the tracking list
	 * 
	 * @param thisFile
	 * @param isInitialLoading
	 * @param collectionToAddTo
	 * @return the response from the listener
	 */
	private int conditionallyAddFileToCollectionAfterContactingListener(File thisFile, boolean isInitialLoading, Collection<File> collectionToAddTo)
	{
		String fileContents = "[BINARYDATA]";
		if (!thisFile.getName().endsWith(PostProductionHandler.EXPECTED_FILE_EXTENSION)) // these
																							// get
																							// too
																							// big
																							// to
																							// parse
		{
			fileContents = FileUtilities.readAllFromFile(thisFile);
		}

		int response = loadedFileListener.loadFileResponse(new LoadedFileEvent(thisFile.getName(), thisFile.getAbsolutePath(), fileContents, isInitialLoading));

		if (response == LoadedFileListener.NO_COMMENT)
		{
			logger.trace("Was given the go to track " + thisFile);
			collectionToAddTo.add(thisFile);
		}
		else
		{
			logger.debug("Was told not to track " + thisFile);
			filesToIgnore.add(thisFile);
		}
		return response;

	}

}
