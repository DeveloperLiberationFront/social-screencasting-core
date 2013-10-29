package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.VideoEncodingException;

public interface WebQueryInterface {

	List<ToolUsage> getAllToolUsagesForPlugin(String pluginName);

	List<String> getNamesOfAllPlugins();

	File extractVideoForLastUsageOfTool(String pluginName, String toolName) throws VideoEncodingException;

}
