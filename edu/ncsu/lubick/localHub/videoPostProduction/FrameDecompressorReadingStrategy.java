package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public interface FrameDecompressorReadingStrategy 
{

	/**
	 * Sets the codec up to continue reading any data necessary at the beginning of the file
	 * before the frame data begins.
	 * @param inputStream
	 * @throws IOException
	 */
	void readInFileHeader(InputStream inputStream) throws IOException;

	Date readInFrameTimeStamp(InputStream inputStream) throws IOException;

	int readFrameType(InputStream inputStream) throws IOException;

	void decompressFrameDataToStream(InputStream inputStream, ByteArrayOutputStream bO) throws IOException, ReachedEndOfCapFileException;
	
	
}
