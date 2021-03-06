package edu.ncsu.dlf.localHub.videoPostProduction.outputs;

import java.io.File;

import edu.ncsu.dlf.localHub.ToolUsage;
import edu.ncsu.dlf.localHub.videoPostProduction.MediaEncodingException;

public interface PreAnimationImagesToMediaOutput
{
	String getMediaTypeInfo();

	File combineImageFilesToMakeMedia(ToolUsage toolUsage, int startIndex, int endIndex) throws MediaEncodingException;

	void setSortedFrames(File[] sortedFrameFiles);
}
