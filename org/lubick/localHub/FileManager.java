package org.lubick.localHub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This is the Runnable that operates on the background thread.
 * 
 * It constantly monitors the Monitor Folder and if anything is detected,
 * the registered LoadedFileListener is called (This is going to be the local hub)
 * @author Kevin Lubick
 *
 */
public class FileManager implements Runnable {

	private Set<File> filesFromLastTime = new HashSet<>();
	/**Maps a string, the name of a plugin, to the queue of as of yet unparsed files related to that plugin*/
	private Map<String, Queue<File>> unparsedFiles = new HashMap<>();
	
	private File monitorDirectory = null;
	private LoadedFileListener loadedFileListener = null;
	private ToolStreamFileParser fileParser = null;
	
	private static Logger logger = Logger.getLogger(FileManager.class.getName());

	public FileManager(LoadedFileListener loadedFileListener, ToolStreamFileParser fileParser) {
		this.loadedFileListener = loadedFileListener;
		this.fileParser = fileParser;
	}

	@Override
	public void run() {
		if (monitorDirectory == null)
		{
			logger.error("LocalHubRunnable did not have a monitor folder. Thread terminating.");
			return;
		}
		unparsedFiles.clear();
		
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
				else if (!filesFromLastTime.contains(child));
				{
					logger.debug("Found new file "+child);
					newFiles.add(child);
				}
			}
			//All the new files have been found in this iteration
			for (File newFile : newFiles) 
			{
				conditionallyAddFileAfterContactingListener(newFile, false, filesFromLastTime);
				parseOrQueueFileAfterNotifyingListener(newFile);
			}

			//Sleep for a second and then do it all again
			try {
				Thread.sleep(1000);		//wake every second
			} catch (InterruptedException e) {
				logger.error("There was an interruption on the main thread",e);
			}
		}

	}

	/**
	 * Takes a file, parses off the name and the time and begins to parse any files older than it
	 * 
	 * @param newFile
	 */
	private void parseOrQueueFileAfterNotifyingListener(File file) 
	{
		if (file == null)
		{
			return;
		}
		String fileName = file.getName();
		int splitPoint = fileName.indexOf('.');
		if (splitPoint == -1)
		{
			logger.info("File "+file+" can be ignored.  Improperly formated.");
			return;
		}
		String pluginName = fileName.substring(0, splitPoint);
		logger.debug(file + " was seen to belong to the plugin "+ pluginName);
		
		Queue<File> filesToParse = unparsedFiles.get(pluginName);
		if (filesToParse == null)
		{
			logger.trace("Created queue for the plugin " + pluginName +" with file "+ file);
			Queue<File> newQueue = new LinkedList<File>();
			newQueue.offer(file);
			unparsedFiles.put(fileName, newQueue);
		}
		else
		{
			filesToParse.offer(file);
			//If this is the only file we haven't parsed yet, it may not be fully written yet, so hold off
			if (filesToParse.size() != 1)
			{
				
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

		if (response == LoadedFileListener.NO_COMMENT)
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
