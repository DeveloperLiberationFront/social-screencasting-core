package edu.ncsu.lubick.localHub.videoPostProduction.gif;

import java.io.File;
import java.io.IOException;

import edu.ncsu.lubick.localHub.videoPostProduction.ImagesToMediaOutput;

public class ImagesToGifOutput implements ImagesToMediaOutput {

	@Override
	public File combineImageFilesToMakeMedia(String fileNameMinusExtension) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMediaTypeInfo()
	{
		return "Video/gif";
	}

}
