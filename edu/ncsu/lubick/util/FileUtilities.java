package edu.ncsu.lubick.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ImproperlyEncodedDateException;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

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
		}
		catch (IOException e)
		{
			logger.error("Error reading in file", e);
			bytes = "There was a problem reading the file".getBytes();
		}
		return new String(bytes);
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
	private static Date extractStartTime(String fileName, SimpleDateFormat formatter) throws ImproperlyEncodedDateException
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
		return extractStartTime(frame.getName(), makeDateInMillisToNumberFormatter());
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
			return "frame."+formatterForFrames.format(date)+"."+PostProductionHandler.INTERMEDIATE_FILE_FORMAT;	
		}
	}

	public static String makeFolderNameForBrowserMediaPackage(ToolUsage tu, String userEmail)
	{
		if (tu == null)
		{
			logger.info("Got a null toolusage, recovering with empty string");
			return PostProductionHandler.MEDIA_OUTPUT_FOLDER;
		}
		return PostProductionHandler.MEDIA_OUTPUT_FOLDER + ToolStream.makeUniqueIdentifierForToolUsage(tu, userEmail);
	}
	
	@Deprecated
	private static String createNumberForMediaOutput(ToolUsage tu)
	{
		int startingPoint = FileUtilities.createHashFromToolName(tu.getToolName());
		return ""+startingPoint +"_"+tu.getTimeStamp().getTime();
	}

	@Deprecated
	public static int createHashFromToolName(String toolName)
	{
		int retval = toolName.hashCode();
		if (toolName.hashCode() == Integer.MIN_VALUE)
			retval = 0;
		return Math.abs(retval);
	}

	@Deprecated
	public static String makeFileNameStemNoDateForToolPluginMedia(String pluginName, String toolName)
	{
		if (toolName == null)
		{
			logger.info("Got a null toolname, recovering with empty string");
			toolName = "";
		}
		return PostProductionHandler.MEDIA_OUTPUT_FOLDER + pluginName + createHashFromToolName(toolName) + "_";
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

}
