package edu.ncsu.lubick.localHub;

import java.util.HashSet;
import java.util.Set;

public class LoadedFileListenerAggregator implements LoadedFileListener {

	private Set<LoadedFileListener> loadedFileListeners = new HashSet<>();
	
	@Override
	public int loadFileResponse(LoadedFileEvent e)
	{
		int retVal = LoadedFileListener.NO_COMMENT;
		for (LoadedFileListener lfl : loadedFileListeners)
		{
			int response = lfl.loadFileResponse(e);
			// return the most extreme value
			if (response > retVal)
			{
				retVal = response;
			}
		}
		return retVal;
	}

	public void add(LoadedFileListener loadedFileListener)
	{
		this.loadedFileListeners.add(loadedFileListener);
	}

	public void remove(LoadedFileListener loadedFileListener)
	{
		this.loadedFileListeners.remove(loadedFileListener);
	}
	
}
