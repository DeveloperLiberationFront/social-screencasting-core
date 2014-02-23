package edu.ncsu.lubick.localHub.forTesting;

import java.util.List;

import edu.ncsu.lubick.localHub.LocalHubProcess;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface LocalHubDebugAccess extends LocalHubProcess{



	List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	

	List<String> getAllPluginNames();

}
