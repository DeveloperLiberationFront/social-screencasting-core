package org.lubick.localHub.forTesting;

import org.lubick.localHub.LoadedFileListener;

public interface LocalHubDebugAccess {

	void addLoadedFileListener(LoadedFileListener loadedFileListener);
	void removeLoadedFileListener(LoadedFileListener loadedFileListener);

	boolean isRunning();

	

}
