package edu.ncsu.lubick.util;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class BlockingImageDiskWritingStrategy extends DefaultImageDiskWritingStrategy
{

	private static Logger logger = Logger.getLogger(BlockingImageDiskWritingStrategy.class.getName());
	private ZipOutputStream out;

	public BlockingImageDiskWritingStrategy(File outputDirectory, boolean deleteImagesAfterUse)
	{
		super(outputDirectory, deleteImagesAfterUse);
		
		File file = new File("video.zip");
		try
		{
			if (file.exists() || file.createNewFile())
			{
				out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			}
		}
		catch (IOException e)
		{
			logger.fatal(e);
		}
	}

	@Override
	public void writeImageToDisk(BufferedImage image) throws IOException
	{
		File f = new File(workingDir, getNextFileName()); // gets filenames from listener
		/*if (!f.createNewFile())
		{
			logger.debug("The image file already exists, going to overwrite");
		}*/
		writeImageToDisk(image, f);
	}

	@Override
	public void writeImageToDisk(BufferedImage image, File outputFile) throws IOException
	{
		logger.trace("Starting write to disk");
		ZipEntry entry=new ZipEntry(outputFile.getName());
		out.putNextEntry(entry);
		ImageIO.write(image, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, out);
		out.closeEntry();
		logger.trace("Finished write to disk");
		if (deleteImagesAfterUse)
		{
			// if we are in a debug state, we want to see the files at the end of all of this.
			outputFile.deleteOnExit();
		}
	}

	@Override
	public void waitUntilDoneWriting()
	{
		// This is a blocking implementation, so we are already done.
	}

	@Override
	public Logger getLogger()
	{
		return logger;
	}

	@Override
	public void resetWithOutClearingFolder()
	{
		// Do nothing
	}
}
