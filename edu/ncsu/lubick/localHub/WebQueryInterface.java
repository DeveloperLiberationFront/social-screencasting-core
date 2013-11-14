package edu.ncsu.lubick.localHub;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;

public interface WebQueryInterface {

	List<ToolUsage> getAllToolUsagesForPlugin(String pluginName);

	List<String> getNamesOfAllPlugins();

	ToolUsage extractMediaForLastUsageOfTool(String pluginName, String toolName) throws MediaEncodingException;

	ToolUsage getLastInstanceOfToolUsage(String pluginName, String toolName);

}
