package org.lubick.localHub.forTesting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

public class TestUtilities {

	private TestUtilities() {}
	private static Logger logger = Logger.getLogger(TestUtilities.class.getName());


	/**
	 * Creates a file in the given directory with the given fileName and then writes
	 * the fileContents to disk.
	 * 
	 * If the file already exists, the file will be deleted and overwritten.
	 * @param directory
	 * @param fileName
	 * @param fileContents
	 * @return
	 */
	public static File createAbsoluteFileWithContent(String directory, String fileName, String fileContents) {
		File newFile = new File(directory, fileName);
		if (newFile.exists()) 
		{
			logger.debug(newFile.toString() + " already exists, deleting first");
			if (!newFile.delete())
			{
				logger.info("Could not overwrite file "+newFile.toString());
			}
		}
		try 
		{
			if (!newFile.createNewFile())
			{
				logger.error("Could not create file "+newFile.toString());
			}
		} 
		catch (IOException e) 
		{
			logger.error("Could not create file", e);
			return null;
		}

		//Using try with resources.  This automatically closes up afterwards, ignoring(?) thrown exception
		try(FileOutputStream fos = new FileOutputStream(newFile);) 
		{
			fos.write(fileContents.getBytes());
		} catch (IOException e) {
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
		if (!rootDirectory.exists() || (rootDirectory.isDirectory() && rootDirectory.listFiles().length ==0))
		{
			return true;
		}
		return recursivelyClearDirectory(rootDirectory);
	}


	private static boolean recursivelyClearDirectory(File parentDirectory) 
	{
		for(File f: parentDirectory.listFiles())
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

}
