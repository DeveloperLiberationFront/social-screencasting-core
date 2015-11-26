package edu.ncsu.dlf.localHub;

public class ImproperlyEncodedDateException extends Exception {

	private static final long serialVersionUID = -8095121386922480897L;

	public ImproperlyEncodedDateException()
	{
		super();
	}

	public ImproperlyEncodedDateException(String message)
	{
		super(message);
	}

	public ImproperlyEncodedDateException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ImproperlyEncodedDateException(Throwable cause)
	{
		super(cause);
	}

}
