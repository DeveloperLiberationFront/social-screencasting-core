package org.lubick.localHub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.log4j.Logger;

public class FileUtilities 
{
	private static Logger logger = Logger.getLogger(FileUtilities.class.getName());
	
	
	private FileUtilities() {}

	public static String readAllFromFile(File fileToParse) {
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(fileToParse.toPath());
		} catch (IOException e) {
			logger.error("Error reading in file",e);
			bytes = "There was a problem reading the file".getBytes();
		}
		return new String(bytes);
	}
	
}
