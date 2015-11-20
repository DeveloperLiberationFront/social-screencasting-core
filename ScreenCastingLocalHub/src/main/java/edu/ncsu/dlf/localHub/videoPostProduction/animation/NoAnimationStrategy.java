package edu.ncsu.dlf.localHub.videoPostProduction.animation;

import edu.ncsu.dlf.localHub.ToolUsage;
import edu.ncsu.dlf.localHub.videoPostProduction.PostProductionAnimationStrategy;

/**
 * does zero additional animation
 * 
 * @author KevinLubick
 * 
 */
public class NoAnimationStrategy implements PostProductionAnimationStrategy {

	public NoAnimationStrategy()
	{
	}

	@Override
	public void addAnimationToImagesInScratchFolderForToolStream(ToolUsage currentToolStream)
	{
		//No animation required
	}

}
