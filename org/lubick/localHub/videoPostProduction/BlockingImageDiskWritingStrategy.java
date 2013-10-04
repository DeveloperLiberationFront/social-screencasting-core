package org.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

public class BlockingImageDiskWritingStrategy extends DefaultImageDiskWritingStrategy
{
	
	private static Logger logger = Logger.getLogger(BlockingImageDiskWritingStrategy.class.getName());
	

	
	public BlockingImageDiskWritingStrategy(String baseDirectoryName, boolean deleteImagesAfterUse) {
		super(baseDirectoryName, deleteImagesAfterUse);
	}

	@Override
	public void writeImageToDisk(BufferedImage readFrame) throws IOException {
		File f = new File(workingDir,getNextFileName());
		if (!f.createNewFile())
		{
			logger.debug("The image file already exists, going to overwrite");
		}
		logger.trace("Starting write to disk");
		ImageIO.write(readFrame, "png", f);
		logger.trace("Finished write to disk");
		if (deleteImagesAfterUse)
		{
			//if we are in a debug state, we want to see the files at the end of all of this.
			f.deleteOnExit();
		}
	}

	@Override
	public void waitUntilDoneWriting() {
		//This is a blocking implementation, so we are already done.
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
}