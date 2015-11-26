package edu.ncsu.dlf.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ncsu.dlf.localHub.ImproperlyEncodedDateException;
import edu.ncsu.dlf.localHub.ToolUsage;
import edu.ncsu.dlf.localHub.videoPostProduction.PostProductionHandler;

public class FileUtilities
{
	private static Logger logger = Logger.getLogger(FileUtilities.class.getName());
	
	private static DateFormat formatterForFrames = makeDateInMillisToNumberFormatter();
	private static DateFormat formatterForLogFiles = makeDateInMinutesToNumberFormatter();
	

	private FileUtilities()
	{
		
	}

	public static String readAllFromFile(File fileToParse)
	{
		byte[] bytes; 
		try
		{
			bytes = Files.readAllBytes(fileToParse.toPath());
			return new String(bytes, StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			logger.error("Error reading in file", e);
			return "There was a problem reading the file";
		}
	}

	public static String padIntTo4Digits(int i)
	{
		if (i < 0)
		{
			logger.error("Who put a negative here? " + i);
			return padIntTo4Digits(Math.abs(i));
		}
		if (i < 10)
		{
			return "000" + i;
		}
		if (i < 100)
		{
			return "00" + i;
		}
		if (i < 1000)
		{
			return "0" + i;
		}
		return String.valueOf(i);
	}

	private static SimpleDateFormat makeDateInMillisToNumberFormatter()
	{
		return new SimpleDateFormat("DDDyykkmmssSSS");
	}
	
	public static SimpleDateFormat makeDateInSecondsToNumberFormatter()
	{
		return new SimpleDateFormat("DDDyykkmmss");
	}

	private static SimpleDateFormat makeDateInMinutesToNumberFormatter()
	{
		return new SimpleDateFormat("DDDyykkmm");
	}

	// Expecting name convention
	// screencasts.ENCODEDDATE.cap
	// OR
	// PLUGINNAME.ENCODEDDATE.log
	private static Date extractStartTime(String fileName, DateFormat formatter) throws ImproperlyEncodedDateException
	{
		if (fileName.indexOf('.') < 0 || fileName.lastIndexOf('.') == fileName.indexOf('.'))
		{
			throw new ImproperlyEncodedDateException(
					"Improperly formatted file name:  Should be like PLUGINNAME.ENCODEDDATE.log or screencast.ENCODEDDATE.cap, was " + fileName);
		}
		String dateString = fileName.substring(fileName.indexOf('.') + 1, fileName.lastIndexOf('.'));
	
		Date associatedDate = null;
		try
		{
			associatedDate = formatter.parse(dateString);
		}
		catch (ParseException e)
		{
			throw new ImproperlyEncodedDateException("Trouble parsing Date " + dateString, e);
		}
		return associatedDate;
	}

	public static Date parseStartDateOfToolStream(File fileToParse) throws ImproperlyEncodedDateException
	{
		return extractStartTime(fileToParse.getName(), makeDateInMinutesToNumberFormatter());
	}

	public static Date parseDateOfMediaFrame(File frame) throws ImproperlyEncodedDateException
	{
		synchronized (formatterForFrames)
		{
			return extractStartTime(frame.getName(), formatterForFrames);
		}
	}

	public static Date parseDateOfMediaFrame(String frameName) throws ImproperlyEncodedDateException
	{
		synchronized (formatterForFrames)
		{
			return extractStartTime(frameName, formatterForFrames);
		}
		
	}

	public static String encodeLogFileName(String pluginName, Date date)
	{
		synchronized (formatterForLogFiles)
		{
			return pluginName +"."+formatterForLogFiles.format(date)+".log";
		}
	}

	public static String encodeMediaFrameName(Date date)
	{
		synchronized (formatterForFrames)
		{
			return "frame."+formatterForFrames.format(date)+"."+PostProductionHandler.FULLSCREEN_IMAGE_FORMAT;	
		}
	}

	/**
	 * Makes a String representing the path name to where this file would exist on disk.
	 * 
	 * This includes the reference to the PostProductionHandler.MEDIA_OUTPUT_FOLDER "renderedVideos"
	 * @param tu
	 * @param userEmail
	 * @return
	 */
	public static String makeLocalFolderNameForBrowserMediaPackage(ToolUsage tu, String userEmail)
	{
		if (tu == null)
		{
			logger.info("Got a null toolusage, recovering with empty string");
			return "";
		}
		return PostProductionHandler.MEDIA_OUTPUT_FOLDER + tu.makeUniqueIdentifierForToolUsage(userEmail);
	}
	

	public static File copyFileToDir(File sourceFile, File destFolder) throws IOException
	{
		if (!destFolder.isDirectory() || !destFolder.exists())
		{
			logger.error("Can't copy to "+destFolder+"!  It's not a directory or doesn't exist");
			return null;
		}
		if (sourceFile==null||!sourceFile.exists())
		{
			logger.error("Can't copy "+sourceFile+"!  It's null or doesn't exist");
			return null;
		}
		File newFile = new File(destFolder,sourceFile.getName());
		
		return Files.copy(sourceFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING).toFile();
		
	}
	
	public static File[] nonNull(File[] maybeNullFileArray) {
		if (maybeNullFileArray == null) {
			return new File[0];
		}
		return maybeNullFileArray;
	}
	
	public static String[] nonNull(String[] maybeNullStringArray) {
		if (maybeNullStringArray == null) {
			return new String[0];
		}
		return maybeNullStringArray;
	}

}
