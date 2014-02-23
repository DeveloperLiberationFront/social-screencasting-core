package edu.ncsu.lubick.localHub.forTesting;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

public class TestingUtils {

	private TestingUtils()
	{
	}

	private static Logger logger = Logger.getLogger(TestingUtils.class.getName());

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


	public static void unzipFileToFolder(File dummyScreencastZip, File testScreencastFolder)
	{
		//From http://kodejava.org/how-do-i-decompress-a-zip-file-using-zipinputstream/
		try (FileInputStream fis = new FileInputStream(dummyScreencastZip);ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));)
		{
			ZipEntry entry;

			while ((entry = zis.getNextEntry()) != null) 
			{
				logger.debug("Unzipping: " + entry.getName());

				int size;
				byte[] buffer = new byte[2048];

				File newFile = new File(testScreencastFolder, entry.getName());
				if (!newFile.exists() && !newFile.createNewFile())
				{
					logger.info("Could not make file " +newFile+". Continuing anyway...");
				}
				try(FileOutputStream fos = new FileOutputStream(newFile);BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);)
				{
					while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
						bos.write(buffer, 0, size);
					}
				}
			}

		} 
		catch (IOException e) {
			logger.error("Problem unzipping "+dummyScreencastZip +" into "+testScreencastFolder,e);
		}
	}

}
