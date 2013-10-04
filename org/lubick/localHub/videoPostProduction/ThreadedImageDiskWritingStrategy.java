package org.lubick.localHub.videoPostProduction;

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
	

	public ThreadedImageDiskWritingStrategy(String baseDirectoryName, boolean deleteImagesAfterUse) {
		super(baseDirectoryName, deleteImagesAfterUse);
		
	}

	@Override
	public void reset() {
		super.reset();
		
		if (workingThreadPool != null)
		{
			workingThreadPool.shutdownNow();
		}
		this.workingThreadPool = Executors.newCachedThreadPool(Executors.defaultThreadFactory());
	}
	

	@Override
	public void waitUntilDoneWriting() {
		workingThreadPool.shutdown();
		try {
			workingThreadPool.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			if (workingThreadPool.isTerminated())
			{
				logger.info("Thread pool was interrupted, but all the tasks finished",e);
			}
			else {
				logger.error("Thread pool was interrupted, and not all the tasks finished",e);
			}
		}
	}

	@Override
	public void writeImageToDisk(final BufferedImage tempImage) throws IOException {
		final File f = new File(workingDir,getNextFileName());
		if (!f.createNewFile())
		{
			logger.debug("The image file already exists, going to overwrite");
		}
		logger.trace("Starting write to disk");
		workingThreadPool.submit(new Runnable() {
			
			@Override
			public void run() {
				try {
					ImageIO.write(tempImage, "png", f);
					logger.trace("Finished write to disk");
				} catch (IOException e) {
					logger.error("There was a problem writing an image to disk on a background thread",e);
				}
			}
		});
		
		
		
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
