package org.lubick.localHub.forTesting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

public class TestUtilities {

	private TestUtilities() {}
	private static Logger logger = Logger.getLogger(TestUtilities.class.getName());


	public static void createAbsoluteFileWithContent(String directory, String fileName, String fileContents) {
		File newFile = new File(directory, fileName);
		if (newFile.exists()) 
		{
			logger.debug(newFile.toString() + " already exists, deleting first");
			newFile.delete();
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
			return;
		}
		
		//Using try with resources.  This automatically closes up afterwards, ignoring(?) thrown exception
		try(FileOutputStream fos = new FileOutputStream(newFile);) 
		{
			fos.write(fileContents.getBytes());
		} catch (IOException e) {
			logger.error("Could not write message to file", e);
		}
		

	}

}
