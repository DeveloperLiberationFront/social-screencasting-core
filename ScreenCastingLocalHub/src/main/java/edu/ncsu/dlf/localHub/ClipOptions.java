package edu.ncsu.dlf.localHub;

import java.awt.Rectangle;

public class ClipOptions {
	public int startFrame;
	public int endFrame;
	public Rectangle cropRect;
	public String shareWithEmail = "public";
	
	public ClipOptions(String shareWithEmail, int startFrame, int endFrame, Rectangle cropRect)
	{
		this.startFrame = startFrame;
		this.endFrame = endFrame;
		this.cropRect = cropRect;
		//dummy check for "public" instead of "all" (happened in earlier version)
		this.shareWithEmail = "all".equals(shareWithEmail) ? "public" : shareWithEmail;
	}

	public ClipOptions(String shareWithEmail, int startFrame, int endFrame)
	{
		this(shareWithEmail, startFrame, endFrame, null);
	}
	
	public ClipOptions(int startFrame, int endFrame)
	{
		this("", startFrame, endFrame, null);		//meant for updating in the database
	}
	
	public ClipOptions(String shareWithEmail)
	{
		this(shareWithEmail, 0, 0, null);
	}
	
	public ClipOptions()
	{
		//Default values of 0 and null are fine
	}
	
	public void update(ClipOptions co) 
	{
		if (!co.shareWithEmail.isEmpty()) {
			this.shareWithEmail = co.shareWithEmail;
		}
		this.startFrame = co.startFrame;
		this.endFrame = co.endFrame;
		this.cropRect = co.cropRect;
	}
}