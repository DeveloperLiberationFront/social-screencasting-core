package edu.ncsu.lubick.localHub;

import java.awt.Rectangle;

public class ClipOptions {
	public int startFrame;
	public int endFrame;
	public Rectangle cropRect;

	public ClipOptions(int startFrame, int endFrame)
	{
		this.startFrame = startFrame;
		this.endFrame = endFrame;
	}
	
	public ClipOptions(int startFrame, int endFrame, Rectangle cropRect)
	{
		this.startFrame = startFrame;
		this.endFrame = endFrame;
		this.cropRect = cropRect;
	}
	
	public ClipOptions()
	{
		//Default values of 0 are fine
	}
}