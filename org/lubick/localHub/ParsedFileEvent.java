package org.lubick.localHub;

import java.util.Date;

/**
 * An event that is triggered when a toolstream file is being parsed.
 * 
 * This is different from when it has been detected, which produces a
 * LoadedFileEvent.  
 * @author Kevin Lubick
 *
 */
public class ParsedFileEvent 
{

	public String getInputJSON() {
		// TODO Auto-generated method stub
		return null;
	}

	public ToolStream getToolStream() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPluginName() {
		// TODO Auto-generated method stub
		return "blarg";
	}

	public Date getFileTimeStamp() {
		// TODO Auto-generated method stub
		return null;
	}



}
