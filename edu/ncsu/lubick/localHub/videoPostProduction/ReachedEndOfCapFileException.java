package edu.ncsu.lubick.localHub.videoPostProduction;

public class ReachedEndOfCapFileException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5950960506267034448L;

	public ReachedEndOfCapFileException() {
	}

	public ReachedEndOfCapFileException(String arg0) {
		super(arg0);
	}

	public ReachedEndOfCapFileException(Throwable arg0) {
		super(arg0);
	}

	public ReachedEndOfCapFileException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ReachedEndOfCapFileException(String arg0, Throwable arg1,
			boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
