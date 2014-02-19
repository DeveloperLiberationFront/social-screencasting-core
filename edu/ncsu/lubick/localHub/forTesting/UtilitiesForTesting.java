package edu.ncsu.lubick.localHub.forTesting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.util.FileUtilities;

public class UtilitiesForTesting {

	private UtilitiesForTesting()
	{
	}

	private static Logger logger = Logger.getLogger(UtilitiesForTesting.class.getName());

	/**
	 * Creates a file in the given directory with the given fileName and then writes the fileContents to disk.
	 * 
	 * If the file already exists, the file will be deleted and overwritten.
	 * 
	 * @param directory
	 * @param fileName
	 * @param fileContents
	 * @return
	 */
	public static File createAbsoluteFileWithContent(String directory, String fileName, String fileContents)
	{
		File newFile = new File(directory, fileName);
		if (newFile.exists())
		{
			logger.debug(newFile.toString() + " already exists, deleting first");
			if (!newFile.delete())
			{
				logger.info("Could not overwrite file " + newFile.toString());
			}
		}
		try
		{
			if (!newFile.createNewFile())
			{
				logger.error("Could not create file " + newFile.toString());
			}
		}
		catch (IOException e)
		{
			logger.error("Could not create file", e);
			return null;
		}

		// Using try with resources. This automatically closes up afterwards,
		// ignoring(?) thrown exception
		try (FileOutputStream fos = new FileOutputStream(newFile);)
		{
			fos.write(fileContents.getBytes());
		}
		catch (IOException e)
		{
			logger.error("Could not write message to file", e);
		}

		return newFile;
	}

	/**
	 * 
	 * @param directory
	 * @return
	 */
	public static boolean clearOutDirectory(String directory)
	{
		File rootDirectory = new File(directory);
		return clearOutDirectory(rootDirectory);
	}

	public static boolean clearOutDirectory(File rootDirectory)
	{
		if (!rootDirectory.exists() || (rootDirectory.isDirectory() && rootDirectory.listFiles().length == 0))
		{
			return true;
		}
		return recursivelyClearDirectory(rootDirectory);
	}

	private static boolean recursivelyClearDirectory(File parentDirectory)
	{
		for (File f : parentDirectory.listFiles())
		{
			if (f.isDirectory())
			{
				if (!recursivelyClearDirectory(f))
					return false;
			}
			if (!f.delete())
			{
				return false;
			}
		}
		return parentDirectory.listFiles().length == 0;
	}

	public static Date truncateTimeToMinute(Date date)
	{
		return truncateTimeToMinute(date.getTime());
	}

	public static Date truncateTimeToMinute(long milliseconds)
	{
		// Divide then multiply by 60000 (the number of milliseconds in a
		// minute) to round to nearest minute
		return new Date((milliseconds / 60000) * 60000);
	}

	public static File copyFileToFolder(String parentDir, String newFileName, File sourceOfFileToCopy) throws IOException
	{
		File destination = new File(parentDir, newFileName);
		if ((destination.exists() && !destination.delete()) || destination.isDirectory())
		{
			logger.error("there was a problem copying the file."
					+ (destination.isDirectory() ? "File was a directory" : "File already exists and won't be deleted"));
			return null;
		}
		if (!destination.createNewFile())
		{
			logger.error("couldn't make the new file to copy to.  Trying to proceed anyway");
		}
		FileOutputStream fos = new FileOutputStream(destination);
		Files.copy(sourceOfFileToCopy.toPath(), fos);

		return destination;
	}

	@Deprecated
	public static String makeFileNameStemForToolPluginMedia(String pluginName, String toolName, Date toolTime)
	{
		if (toolName == null)
		{
			PostProductionHandler.logger.info("Got a null toolname, recovering with empty string");
			toolName = "";
		}
		return PostProductionHandler.MEDIA_OUTPUT_FOLDER + pluginName + FileUtilities.createNumberFromToolName(toolName) + "_"+ toolTime.getTime();
	}

}
