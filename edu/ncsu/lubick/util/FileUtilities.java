package edu.ncsu.lubick.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ImproperlyEncodedDateException;

public class FileUtilities
{
	private static Logger logger = Logger.getLogger(FileUtilities.class.getName());
	
	private static DateFormat formatterForCapFile = makeDateInSecondsToNumberFormatter();
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
			return "I cant deal with negatives";
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

	private static SimpleDateFormat makeDateInSecondsToNumberFormatter()
	{
		return new SimpleDateFormat("DDDyykkmmss");
	}

	private static SimpleDateFormat makeDateInMinutesToNumberFormatter()
	{
		return new SimpleDateFormat("DDDyykkmm");
	}

	public static Date parseStartDateOfCapFile(File capFile) throws ImproperlyEncodedDateException
	{
		return extractStartTime(capFile.getName(), makeDateInSecondsToNumberFormatter());
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

	public static String encodeCapFileName(Date date)
	{
		synchronized (formatterForCapFile)			//SimpleDateFormats are not thread-safe
		{
			return "screencasts."+formatterForCapFile.format(date)+".cap";
		}
	}

	public static String encodeLogFileName(String pluginName, Date date)
	{
		synchronized (formatterForLogFiles)
		{
			return pluginName +"."+formatterForLogFiles.format(date)+".log";
		}
	}

}
