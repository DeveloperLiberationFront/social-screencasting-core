package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;

import org.apache.log4j.Logger;

/**
 * Gives some shared resourses for media outputs, either pre- or post-animated ones
 * 
 * @author KevinLubick
 */
public abstract class AbstractImagesToMediaOutput
{
	protected File scratchDir;

	public AbstractImagesToMediaOutput(File file)
	{
		this.scratchDir = file;
	}

	protected void cleanUpForFile(File newFile) throws MediaEncodingException
	{
		if (!newFile.getParentFile().mkdirs() && !newFile.getParentFile().exists())
		{
			throw new MediaEncodingException("Could not make the output folder " + newFile.getParentFile());
		}

		if (newFile.exists() && !newFile.delete())
		{
			getLogger().error("Could not make video file.  Could not delete previous gif " + newFile);
			throw new MediaEncodingException("Could not make video file.  Could not delete previous gif" + newFile);
		}
	}

	protected abstract Logger getLogger();


	protected File makeDirectoryIfClear(String fileNameMinusExtension) throws MediaEncodingException
	{
		File newDir = new File(fileNameMinusExtension);
		if (newDir.exists() && newDir.isDirectory())
		{
			throw new MediaEncodingException("Not creating new media because folder already exists");
		}
		else if (newDir.exists() && !newDir.isDirectory())
		{
			throw new MediaEncodingException("Not creating new media because a non-directory exists where this should be");
		}
		else if (!newDir.exists() && !newDir.mkdirs()) // makes the dir
		{
			throw new MediaEncodingException("Could not create media folder.  Unknown cause");
		}
		return newDir;
	}

}
