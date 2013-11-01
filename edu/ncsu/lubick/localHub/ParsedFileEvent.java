package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.Date;

/**
 * An event that is triggered when a toolstream file is being parsed.
 * 
 * This is different from when it has been detected, which produces a LoadedFileEvent.
 * 
 * @author Kevin Lubick
 * 
 */
public class ParsedFileEvent
{

	private String inputString;
	private ToolStream generatedToolStream;
	private String associatedPluginName;
	private Date fileTimeStamp;
	private File parsedFile;

	public ParsedFileEvent(String inputString, ToolStream generatedToolStream, String associatedPluginName, Date fileTimeStamp, File parsedFile)
	{
		this.inputString = inputString;
		this.generatedToolStream = generatedToolStream;
		this.associatedPluginName = associatedPluginName;
		this.fileTimeStamp = fileTimeStamp;
		this.parsedFile = parsedFile;
	}

	public String getInputJSON()
	{
		return this.inputString;
	}

	public ToolStream getToolStream()
	{
		return this.generatedToolStream;
	}

	public String getPluginName()
	{
		return this.associatedPluginName;
	}

	public Date getFileTimeStamp()
	{
		return this.fileTimeStamp;
	}

	public File getParsedFile()
	{
		return parsedFile;
	}

}
