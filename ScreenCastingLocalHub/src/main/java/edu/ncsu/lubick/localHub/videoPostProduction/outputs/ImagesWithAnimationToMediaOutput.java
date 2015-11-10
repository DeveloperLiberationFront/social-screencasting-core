package edu.ncsu.lubick.localHub.videoPostProduction.outputs;

import java.io.File;

import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;

public interface ImagesWithAnimationToMediaOutput
{
	public File combineImageFilesToMakeMedia(String fileNameStem) throws MediaEncodingException;

	public String getMediaTypeInfo();

}
