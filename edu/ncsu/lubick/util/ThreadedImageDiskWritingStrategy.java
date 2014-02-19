package edu.ncsu.lubick.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class ThreadedImageDiskWritingStrategy extends DefaultImageDiskWritingStrategy {

	private static final int THREAD_PRIORITY = Thread.NORM_PRIORITY-1;
	private static final int MAX_THREADS = 5;
	private static Logger logger = Logger.getLogger(ThreadedImageDiskWritingStrategy.class.getName());
	private ExecutorService workingThreadPool = null;

	private final AtomicInteger workingThreadCount = new AtomicInteger(0);
	
	public ThreadedImageDiskWritingStrategy(String baseDirectoryName, boolean deleteImagesAfterUse)
	{
		super(baseDirectoryName, deleteImagesAfterUse);


	}

	public ThreadedImageDiskWritingStrategy(File outputDirectory, boolean deleteImagesAfterUse)
	{
		super(outputDirectory, deleteImagesAfterUse);
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
		writeImageToDisk(tempImage, f);

	}

	@Override
	public void writeImageToDisk(final BufferedImage image, final File outputFile)
	{
		logger.trace("Starting write to disk");
		while (workingThreadCount.get() >= MAX_THREADS)
		{
			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException e)
			{
				logger.error("Interrupted while waiting for worker space",e);
			}
		}
		workingThreadPool.submit(new Runnable() {

			@Override
			public void run()
			{
				workingThreadCount.incrementAndGet();
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
				workingThreadCount.decrementAndGet();
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
		this.workingThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
			
			@Override
			public Thread newThread(Runnable arg0)
			{
				Thread t = new Thread(arg0);
				t.setPriority(THREAD_PRIORITY);
				return t;
			}
		});
	}

}
