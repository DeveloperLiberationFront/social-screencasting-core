package org.lubick.localHub.videoPostProduction;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.InflaterInputStream;

import org.apache.log4j.Logger;

/**
 * A decoder that pairs with the default encoding in ScreenCasting Module
 * @author Kevin Lubick
 *
 */
public class DefaultCodec implements FrameDecompressorCodecStrategy 
{
	private static Logger logger = Logger.getLogger(DefaultCodec.class.getName());

	private Rectangle currentFrameRect;
	
	private Date capFileStartTime;

	private FramePacket previousFramePacket = null;
	
	private BufferedImage previousImage = null;

	private int firstFrameTimeStamp;

	@Override
	public void readInFileHeader(InputStream inputStream) throws IOException {
		int width = inputStream.read();
		width = width << 8;
		width += inputStream.read();
		int height = inputStream.read();
		height = height << 8;
		height += inputStream.read();
	
		this.currentFrameRect = new Rectangle(width, height);
	}
	
	@Override
	public BufferedImage readInFrameImage(InputStream inputStream) throws IOException 
	{
		logger.trace("Starting to read in frame");
		FramePacket framePacket = unpackNextFrame(inputStream);
	
		if (framePacket == null) //must have reached the file
		{
			return null;
		}
		logger.debug("read in frame "+framePacket);
	
		//Date frameTime = frame.getFrameTimeStamp();
	
		int result = framePacket.getResult();
		if (result == FramePacket.NO_CHANGES_THIS_FRAME) {
			return previousImage;
		} else if (result == FramePacket.REACHED_END) {
			logger.fatal("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			logger.fatal("I TOTALLY DID NOT EXPECT THIS LINE OF CODE TO BE REACHED");
			return null;
		}
		previousFramePacket = framePacket;
	
		BufferedImage bufferedImage = new BufferedImage(currentFrameRect.width, currentFrameRect.height,
				BufferedImage.TYPE_INT_RGB);
		bufferedImage.setRGB(0, 0, currentFrameRect.width, currentFrameRect.height, framePacket.getData(), 0,
				currentFrameRect.width);
	
		previousImage = bufferedImage;
	
		return bufferedImage;
	}
	
	private FramePacket unpackNextFrame(InputStream inputStream) throws IOException 
	{
	
		FramePacket frame = new FramePacket(currentFrameRect.width * currentFrameRect.height, previousFramePacket);
	
		Date timeStamp = readInFrameTimeStamp(inputStream);
		
		if (timeStamp == null)
		{
			return null;
		}
	
		frame.setFrameTimeStamp(timeStamp);
	
		int type = readFrameType(inputStream);
	
		if (type == FramePacket.NO_CHANGES_THIS_FRAME || type == FramePacket.REACHED_END) 
		{
			frame.setResult(type);
			return frame;
		}
	
		try (ByteArrayOutputStream bO = new ByteArrayOutputStream();) {
	
			byte[] compressedData = readCompressedData(inputStream);
	
			decompressData(bO, compressedData);
	
			frame.setEncodedData(bO.toByteArray());
		} 
		catch (IOException e) 
		{
			logger.error("Problem unpacking ",e);
			frame.setResult(FramePacket.NO_CHANGES_THIS_FRAME);
			return frame;
		}
		frame.runLengthDecode();
		//runLengthDecode(frame);
	
		return frame;
	}
	
	/*
	 * The first four bytes from the data stream 
	 */
	private int readFrameType(InputStream inputStream) throws IOException 
	{
		byte type = (byte) inputStream.read();
		logger.trace("Packed Code:"+type);
		//Convert the type to a positive int.  It shouldn't be otherwise, but just in case
		return (type & 0xFF);
	}

	private byte[] readCompressedData(InputStream inputStream) throws IOException {
	
		int compressedFrameSize = readCompressedFrameSize(inputStream);
	
		byte[] compressedData = new byte[compressedFrameSize];
		int readCursor = 0;
		int sizeRead = 0;
	
		//Read in all the data
		while (sizeRead > -1) {
			readCursor += sizeRead;
			if (readCursor >= compressedFrameSize) {
				break;
			}
	
			sizeRead = inputStream.read(compressedData, readCursor, compressedFrameSize - readCursor);
		}
		return compressedData;
	}

	private int readCompressedFrameSize(InputStream inputStream) throws IOException 
	{
		int readBuffer;
		readBuffer = inputStream.read();
	
		int zSize = readBuffer;
		zSize = zSize << 8;
		readBuffer = inputStream.read();
		zSize += readBuffer;
		zSize = zSize << 8;
		readBuffer = inputStream.read();
		zSize += readBuffer;
		zSize = zSize << 8;
		readBuffer = inputStream.read();
		zSize += readBuffer;
	
		logger.trace("Zipped Frame size:"+zSize);
		return zSize;
	}

	private void decompressData(ByteArrayOutputStream bO, byte[] compressedData) throws IOException {
		int sizeRead;
		ByteArrayInputStream bI = new ByteArrayInputStream(compressedData);
	
		InflaterInputStream zI = new InflaterInputStream(bI);
	
		byte[] buffer = new byte[1000];
		sizeRead = zI.read(buffer);
	
		while (sizeRead > -1) {
			bO.write(buffer, 0, sizeRead);
			bO.flush();
	
			sizeRead = zI.read(buffer);
		}
		bO.flush();
	}
	
	/**
	 * The first part in any frame is the time stamp.  This attempts to read the time stamp, or, if we've reached
	 * the end of the file, will return null and set reachedEOF to true.
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	private Date readInFrameTimeStamp(InputStream inputStream) throws IOException {
		int readBuffer = inputStream.read();
		if (readBuffer < 0)
		{
			return null;
		}
		int timeOffset = readBuffer;
		timeOffset = timeOffset << 8;
		readBuffer = inputStream.read();
		timeOffset += readBuffer;
		timeOffset = timeOffset << 8;
		readBuffer = inputStream.read();
		timeOffset += readBuffer;
		timeOffset = timeOffset << 8;
		readBuffer = inputStream.read();
		timeOffset += readBuffer;
	
		return makeNewFrameTimeStamp(timeOffset);
	}
	
	private Date makeNewFrameTimeStamp(int timeOffset) 
	{
		if (this.firstFrameTimeStamp == -1)
		{
			this.firstFrameTimeStamp = timeOffset;
			return this.capFileStartTime;
		}
		return new Date(this.capFileStartTime.getTime() + (timeOffset - this.firstFrameTimeStamp));
	}

	@Override
	public Date getPreviousFrameTimeStamp() {
		return previousFramePacket.getFrameTimeStamp();
	}

	@Override
	public void setFrameZeroTime(Date newTimeZero) {
		this.capFileStartTime = newTimeZero;
		this.firstFrameTimeStamp=-1;
	}
	
}
