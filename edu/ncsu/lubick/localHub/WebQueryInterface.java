package edu.ncsu.lubick.localHub;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface WebQueryInterface {

	List<ToolUsage> getAllToolUsagesForPlugin(String pluginName);

	List<String> getNamesOfAllPlugins();

}
