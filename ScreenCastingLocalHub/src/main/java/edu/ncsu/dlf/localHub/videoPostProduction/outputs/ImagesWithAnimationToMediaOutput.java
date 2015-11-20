package edu.ncsu.dlf.localHub.videoPostProduction.outputs;

import java.io.File;

import edu.ncsu.dlf.localHub.videoPostProduction.MediaEncodingException;

public interface ImagesWithAnimationToMediaOutput
{
	public File combineImageFilesToMakeMedia(String fileNameStem) throws MediaEncodingException;

	public String getMediaTypeInfo();

}
