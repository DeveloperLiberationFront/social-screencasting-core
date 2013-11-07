package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.IOException;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface PreAnimationImagesToMediaOutput
{

	String getMediaTypeInfo();

	File combineImageFilesToMakeMedia(String fileName, ToolUsage currentToolStream) throws IOException;

}
