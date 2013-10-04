package org.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public interface FrameDecompressorCodecStrategy 
{
	void readInFileHeader(InputStream inputStream) throws IOException;

	BufferedImage readInFrameImage(InputStream inputStream) throws IOException;

	Date getPreviousFrameTimeStamp();

	void setFrameZeroTime(Date capFileStartTime);        
	
}
