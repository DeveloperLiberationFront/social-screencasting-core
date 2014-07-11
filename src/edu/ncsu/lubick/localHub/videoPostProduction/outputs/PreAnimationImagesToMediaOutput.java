package edu.ncsu.lubick.localHub.videoPostProduction.outputs;

import java.io.File;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;

public interface PreAnimationImagesToMediaOutput
{
	String getMediaTypeInfo();

	File combineImageFilesToMakeMedia(ToolUsage toolUsage, int startIndex, int endIndex) throws MediaEncodingException;

	void setSortedFrames(File[] sortedFrameFiles);
}
