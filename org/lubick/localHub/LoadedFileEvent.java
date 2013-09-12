package org.lubick.localHub;

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
