package edu.ncsu.lubick.localHub;

public class ClipOptions {
	public int startFrame;
	public int endFrame;

	public ClipOptions(int startFrame, int endFrame)
	{
		this.startFrame = startFrame;
		this.endFrame = endFrame;
	}
	
	public ClipOptions()
	{
		//Default values of 0 are fine
	}
}