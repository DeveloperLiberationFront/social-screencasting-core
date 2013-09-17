package org.lubick.localHub;

/**
 * An event that is triggered when a (probably) toolstream file has been detected.
 * 
 * This is different from when it is being parsed (which happens later), which produces a
 * ParsedFileEvent.  
 * @author Kevin Lubick
 *
 */
public class LoadedFileEvent {
	
	private String fileName = null;
	private String fileContents = null;
	private boolean wasInitialReadIn = false;

	public LoadedFileEvent(String fileName, String fileContents, boolean wasInitialReadIn) {
		this.fileName = fileName;
		this.fileContents = fileContents;
		this.wasInitialReadIn = wasInitialReadIn;
	}

	public String getFileName() {
		return fileName;
	}

	public String getFileContents() {
		return fileContents;
	}

	public boolean wasInitialReadIn() {
		return wasInitialReadIn;
	}
	

	

}
