package edu.ncsu.lubick.localHub;

public interface LoadedFileListener {

	public static final int NO_COMMENT = 0; // Don't stop
	public static final int DONT_PARSE = 1;

	public int loadFileResponse(LoadedFileEvent e);

}
