package org.lubick.localHub.videoPostProduction;

import java.io.File;

import org.apache.log4j.Logger;
import org.lubick.localHub.FileUtilities;


public abstract class DefaultImageDiskWritingStrategy implements ImageDiskWritingStrategy {

	private int currentTempImageNumber = -1;
	
	protected File workingDir;

	protected boolean deleteImagesAfterUse;
	
	public DefaultImageDiskWritingStrategy(String baseDirectoryName, boolean deleteImagesAfterUse) {
		this.workingDir = new File(baseDirectoryName);
		if (!workingDir.exists() && !workingDir.mkdir())
		{
			getLogger().error("There was a problem making the scratchDirectory for the images");
		}
		this.deleteImagesAfterUse = deleteImagesAfterUse;
		
		reset();
	}
	

	public abstract Logger getLogger();


	protected String getNextFileName() {
		currentTempImageNumber++;
		return "temp"+FileUtilities.padIntTo4Digits(currentTempImageNumber)+".png";
	}

	@Override
	public void reset() {
		currentTempImageNumber = -1;
		
		for(File file:workingDir.listFiles())
		{
			if (file.getName().endsWith(".png"))
			{
				if (!file.delete())
				{
					throw new RuntimeException("Could not clear out old png files");
				}
			}
		}
	}


}