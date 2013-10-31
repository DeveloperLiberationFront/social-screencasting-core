package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

public abstract class AbstractImagesToMediaOutput implements ImagesToMediaOutput 
{
	protected File scratchDir;
	public AbstractImagesToMediaOutput(File file)
	{
		this.scratchDir = file;
	}

	protected void cleanUpForFile(File newFile) throws IOException
	{
		if (!newFile.getParentFile().mkdirs() && !newFile.getParentFile().exists())
		{
			throw new IOException("Could not make the output folder " + newFile.getParentFile());
		}

		if (newFile.exists() && !newFile.delete())
		{
			getLogger().error("Could not make video file.  Could not delete previous gif " + newFile);
			throw new IOException("Could not make video file.  Could not delete previous gif" + newFile);
		}
	}

	protected abstract Logger getLogger();

	protected File[] getImageFilesToAnimate()
	{
		File[] imagesToAnimate = scratchDir.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname)
			{
				return pathname.getName().endsWith(PostProductionHandler.INTERMEDIATE_FILE_FORMAT);
			}
		});
		Arrays.sort(imagesToAnimate);
		return imagesToAnimate;
	}

}
