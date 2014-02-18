package edu.ncsu.lubick.localHub.forTesting;

import java.io.File;
import java.util.List;

import edu.ncsu.lubick.localHub.LoadedFileListener;
import edu.ncsu.lubick.localHub.ParsedFileListener;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;

public interface LocalHubDebugAccess {

	boolean isRunning();

	// Listeners
	void addLoadedFileListener(LoadedFileListener loadedFileListener);

	void removeLoadedFileListener(LoadedFileListener loadedFileListener);

	void addParsedFileListener(ParsedFileListener parsedFileListener);

	void removeParsedFileListener(ParsedFileListener parsedFileListener);

	List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	void shutDown();

	List<File> extractVideoForLastUsageOfTool(String pluginName, String toolName) throws MediaEncodingException;

	List<String> getAllPluginNames();

}
