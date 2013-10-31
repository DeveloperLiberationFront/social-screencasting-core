package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

public class ThreadedImageDiskWritingStrategy extends DefaultImageDiskWritingStrategy {

	private static Logger logger = Logger.getLogger(ThreadedImageDiskWritingStrategy.class.getName());
	private ExecutorService workingThreadPool = null;

	public ThreadedImageDiskWritingStrategy(String baseDirectoryName, boolean deleteImagesAfterUse)
	{
		super(baseDirectoryName, deleteImagesAfterUse);

	}

	@Override
	public void reset()
	{
		super.reset();

		resetWithOutClearingFolder();
	}

	@Override
	public void waitUntilDoneWriting()
	{
		workingThreadPool.shutdown();
		try
		{
			workingThreadPool.awaitTermination(60, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			if (workingThreadPool.isTerminated())
			{
				logger.info("Thread pool was interrupted, but all the tasks finished", e);
			}
			else
			{
				logger.error("Thread pool was interrupted, and not all the tasks finished", e);
			}
		}
	}

	@Override
	public void writeImageToDisk(final BufferedImage tempImage) throws IOException
	{
		final File f = new File(workingDir, getNextFileName());
		if (!f.createNewFile())
		{
			logger.debug("The image file already exists, going to overwrite");
		}
		writeImageToDisk(tempImage, f);

	}

	@Override
	public void writeImageToDisk(final BufferedImage image, final File outputFile)
	{
		logger.trace("Starting write to disk");
		workingThreadPool.submit(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					ImageIO.write(image, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, outputFile);
					logger.trace("Finished write to disk");
					if (deleteImagesAfterUse)
					{
						// if we are in a debug state, we want to see the files
						// at the end of all of this.
						outputFile.deleteOnExit();
					}
				}
				catch (IOException e)
				{
					logger.error("There was a problem writing an image to disk on a background thread", e);
				}
			}
		});
	}

	@Override
	public Logger getLogger()
	{
		return logger;
	}

	@Override
	public void resetWithOutClearingFolder()
	{
		if (workingThreadPool != null)
		{
			workingThreadPool.shutdownNow();
		}
		this.workingThreadPool = Executors.newCachedThreadPool();
	}
	

}
