package edu.ncsu.lubick.localHub.forTesting;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface LocalHubDebugAccess {

	boolean isRunning();

	List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	void shutDown();

	List<String> getAllPluginNames();

}
