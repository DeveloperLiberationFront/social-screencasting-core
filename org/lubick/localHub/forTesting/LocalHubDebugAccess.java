package org.lubick.localHub.forTesting;

import java.util.List;

import org.lubick.localHub.LoadedFileListener;
import org.lubick.localHub.ParsedFileListener;
import org.lubick.localHub.ToolStream.ToolUsage;

public interface LocalHubDebugAccess {

	boolean isRunning();
	
	//Listeners
	void addLoadedFileListener(LoadedFileListener loadedFileListener);
	void removeLoadedFileListener(LoadedFileListener loadedFileListener);

	
	void addParsedFileListener(ParsedFileListener parsedFileListener);
	void removeParsedFileListener(ParsedFileListener parsedFileListener);

	List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	void shutDown();
	

}
