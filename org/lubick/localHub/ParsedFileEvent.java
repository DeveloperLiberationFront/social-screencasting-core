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

	private String inputString;
	private ToolStream generatedToolStream;
	private String associatedPluginName;
	private Date fileTimeStamp;

	public String getInputJSON() {
		return this.inputString;
	}

	public ToolStream getToolStream() {
		return this.generatedToolStream;
	}

	public String getPluginName() {
		return this.associatedPluginName;
	}

	public Date getFileTimeStamp() {
		return this.fileTimeStamp;
	}

	

}
