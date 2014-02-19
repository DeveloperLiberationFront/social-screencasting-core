package edu.ncsu.lubick.localHub.forTesting;

import java.util.List;

import edu.ncsu.lubick.localHub.ParsedFileListener;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface LocalHubDebugAccess {

	boolean isRunning();

	// Listeners

	void addParsedFileListener(ParsedFileListener parsedFileListener);

	void removeParsedFileListener(ParsedFileListener parsedFileListener);

	List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	void shutDown();

	List<String> getAllPluginNames();

}
