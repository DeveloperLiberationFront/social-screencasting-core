package org.lubick.localHub.forTesting;

import org.lubick.localHub.LoadedFileListener;
import org.lubick.localHub.ParsedFileListener;

public interface LocalHubDebugAccess {

	boolean isRunning();
	
	//Listeners
	void addLoadedFileListener(LoadedFileListener loadedFileListener);
	void removeLoadedFileListener(LoadedFileListener loadedFileListener);

	
	void addParsedFileListener(ParsedFileListener parsedFileListener);
	void removeParsedFileListener(ParsedFileListener parsedFileListener);
	

}
