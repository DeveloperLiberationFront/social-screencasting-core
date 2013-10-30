package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionAnimationStrategy;

/**
 * does zero additional animation
 * 
 * @author KevinLubick
 * 
 */
public class DefaultAnimationStrategy implements PostProductionAnimationStrategy {

	public DefaultAnimationStrategy(String scratchDir)
	{
	}

	@Override
	public void addAnimationToImagesInScratchFolderForToolStream(ToolUsage currentToolStream)
	{
	}

}
