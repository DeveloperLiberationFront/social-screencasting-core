package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.IOException;

import edu.ncsu.lubick.localHub.ToolUsage;

public interface PostProductionAnimationStrategy {

	void addAnimationToImagesInScratchFolderForToolStream(ToolUsage currentToolStream) throws IOException;

}
