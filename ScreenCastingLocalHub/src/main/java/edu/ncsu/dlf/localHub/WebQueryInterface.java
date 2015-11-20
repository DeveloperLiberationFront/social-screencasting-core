package edu.ncsu.dlf.localHub;

import java.io.File;
import java.util.List;

import edu.ncsu.dlf.util.ToolCountStruct;

public interface WebQueryInterface {

	List<String> getNamesOfAllApplications();

	List<File> getBestExamplesOfTool(String applicationName, String toolName, boolean isKeyboardHuh);

	void shareClipWithUser(String clipId, ClipOptions clipOptions);

	List<ToolCountStruct> getAllToolAggregateForApplication(String applicationName);
	
	ToolCountStruct getToolAggregate(String applicationName, String toolName);
	
	void userPause(boolean shouldPause);
}
