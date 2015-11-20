package edu.ncsu.dlf.localHub.videoPostProduction;

import java.io.IOException;

import edu.ncsu.dlf.localHub.ToolUsage;

public interface PostProductionAnimationStrategy {

	void addAnimationToImagesInScratchFolderForToolStream(ToolUsage currentToolStream) throws IOException;

}
