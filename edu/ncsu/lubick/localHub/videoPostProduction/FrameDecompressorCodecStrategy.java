package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public interface FrameDecompressorCodecStrategy 
{
	/**
	 * Sets the codec up to continue reading any data necessary at the beginning of the file
	 * before the frame data begins.
	 * @param inputStream
	 * @throws IOException
	 */
	void readInFileHeader(InputStream inputStream) throws IOException;

	/**
	 * Reads in the next frame from the input stream
	 * @param inputStream
	 * @return image or null if the end of the file has been reached.
	 * @throws IOException
	 */
	BufferedImage readInFrameImage(InputStream inputStream) throws IOException;

	/**
	 * Sets the first frame in this inputStream's date to be used in conjunction with
	 * the getPreviousFrameTimeStamp()
	 * @param capFileStartTime
	 */
	void setFrameZeroTime(Date capFileStartTime);    
	
	/**
	 * 
	 * @return the Date belonging to previously read in file.  Unspecified behavior if 
	 * a frame hasn't been read in yet.
	 */
	Date getPreviousFrameTimeStamp();
	
}
