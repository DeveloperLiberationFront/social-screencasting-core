package org.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.lubick.localHub.FileUtilities;

public class BlockingImageDiskWritingStrategy implements ImageDiskWritingStrategy 
{
	
	private static Logger logger = Logger.getLogger(DefaultCodec.class.getName());
	
	private int currentTempImageNumber = -1;

	private File workingDir;

	private boolean deleteImagesAfterUse;
	
	public BlockingImageDiskWritingStrategy(String baseDirectoryName, boolean deleteImagesAfterUse) {
		this.workingDir = new File(baseDirectoryName);
		if (!workingDir.exists() && !workingDir.mkdir())
		{
			logger.error("There was a problem making the scratchDirectory for the images");
		}
		this.deleteImagesAfterUse = deleteImagesAfterUse;
	}

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

	private String getNextFileName() 
	{
		currentTempImageNumber++;
		return "temp"+FileUtilities.padIntTo4Digits(currentTempImageNumber)+".png";
	}

	@Override
	public void waitUntilDoneWriting() {
		//This is a blocking implementation, so we are already done.
	}

	@Override
	public void reset() {
		currentTempImageNumber = -1;
	}
}
