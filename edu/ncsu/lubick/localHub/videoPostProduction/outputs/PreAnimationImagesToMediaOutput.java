package edu.ncsu.lubick.localHub.videoPostProduction.outputs;

import java.io.File;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;

public interface PreAnimationImagesToMediaOutput
{
	public File combineImageFilesToMakeMedia(String fileNameStem, ToolUsage currentToolUsage) throws MediaEncodingException;

	public String getMediaTypeInfo();
}
