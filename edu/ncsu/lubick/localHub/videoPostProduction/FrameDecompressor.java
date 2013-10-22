package edu.ncsu.lubick.localHub.videoPostProduction;

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
 * 
 * @author Kevin Lubick
 *
 */
public class FrameDecompressor implements FrameDecompressorCodecStrategy, FrameDecompressorReadingStrategy  
{
	private static final int NORMAL_END_OF_CAP_FILE_COMPRESSED_FRAME_SIZE = -16843009;

	private static Logger logger = Logger.getLogger(FrameDecompressor.class.getName());

	private Rectangle currentFrameRect;
	
	private Date capFileStartTime;

	private DecompressionFramePacket previousFramePacket = null;
	
	private BufferedImage previousImage = null;


	
	private static final int MAX_BLOCK_LENGTH = 126;

	private static final byte STREAK_OF_SAME_AS_LAST_TIME_BLOCKS_CONSTANT = (byte) 0xFF;
	public static final int ALPHA = 0xFF000000;
	
	private int firstFrameTimeStamp;
	
	private FrameDecompressorCodecStrategy fdcs;
	private FrameDecompressorReadingStrategy fdrs;
	
	public FrameDecompressor() 
	{
		this.fdcs = this;
		this.fdrs = this;
	}
	
	public FrameDecompressor(FrameDecompressorCodecStrategy fdcs, FrameDecompressorReadingStrategy fdrs) 
	{
		this.fdcs = fdcs;
		this.fdrs = fdrs;
	}
	
	public BufferedImage readInFrameImage(InputStream inputStream) throws IOException, VideoEncodingException, ReachedEndOfCapFileException 
	{
		logger.trace("Starting to read in frame");
		DecompressionFramePacket framePacket = unpackNextFrame(inputStream);
		
		if (framePacket == null) //must have reached the file
		{
			throw new ReachedEndOfCapFileException();
		}
		
		return this.fdcs.decodeFramePacketToBufferedImage(framePacket);
		
		
	}

	private DecompressionFramePacket unpackNextFrame(InputStream inputStream) throws IOException, VideoEncodingException, ReachedEndOfCapFileException 
	{
	
		DecompressionFramePacket frame = new DecompressionFramePacket(currentFrameRect);
		
		
		Date timeStamp = this.fdrs.readInFrameTimeStamp(inputStream);
		
		if (timeStamp == null)
		{
			return null;
		}
	
		frame.setFrameTimeStamp(timeStamp);
	
		int type = fdrs.readFrameType(inputStream);
	
		if (type == DecompressionFramePacket.NO_CHANGES_THIS_FRAME || type == DecompressionFramePacket.REACHED_END) 
		{
			frame.setResult(type);
			return frame;
		}
	
		try (ByteArrayOutputStream bO = new ByteArrayOutputStream();) {
	
			fdrs.decompressFrameDataToStream(inputStream,bO);
			
			frame.setEncodedData(bO.toByteArray());
		} 
		catch (IOException e) 
		{
			logger.error("Problem unpacking ",e);
			frame.setResult(DecompressionFramePacket.NO_CHANGES_THIS_FRAME);
			return frame;
		}
		catch (RuntimeException e)
		{
			logger.error("Probably a malformed screencapture packet", e);
			throw new VideoEncodingException(e);
		}
	
		return frame;
	}

	public Date getPreviousFrameTimeStamp() {
		return previousFramePacket.getFrameTimeStamp();
	}

	public void setFrameZeroTime(Date newTimeZero) {
		this.capFileStartTime = newTimeZero;
		this.firstFrameTimeStamp=-1;
	}

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
	

	/*
	 * The first four bytes from the data stream 
	 */
	@Override
	public int readFrameType(InputStream inputStream) throws IOException 
	{
		byte type = (byte) inputStream.read();
		logger.trace("Packed Code:"+type);
		//Convert the type to a positive int.  It shouldn't be otherwise, but just in case
		return (type & 0xFF);
	}

	/**
	 * The first part in any frame is the time stamp.  This attempts to read the time stamp, or, if we've reached
	 * the end of the file, will return null and set reachedEOF to true.
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	@Override
	public Date readInFrameTimeStamp(InputStream inputStream) throws IOException {
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

	@Override
	public void decompressFrameDataToStream(InputStream inputStream, ByteArrayOutputStream bO) throws IOException, ReachedEndOfCapFileException {
		byte[] compressedData = readCompressedData(inputStream);
		
		decompressData(bO, compressedData);
		
	}

	@Override
	public BufferedImage decodeFramePacketToBufferedImage(DecompressionFramePacket framePacket) 
	{
		if (framePacket == null)
		{
			logger.error("I got null when decoding.  Returning previous image");
			return previousImage;
		}
		
		framePacket.setPreviousFramePacket(previousFramePacket);
		
		decodeFramePacketUsingRunTimeEncoding(framePacket);
		
		logger.debug("read in frame "+framePacket);
		
		//Date frameTime = frame.getFrameTimeStamp();
	
		int result = framePacket.getResult();
		if (result == DecompressionFramePacket.NO_CHANGES_THIS_FRAME) {
			return previousImage;
		} else if (result == DecompressionFramePacket.REACHED_END) {
			logger.fatal("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			logger.fatal("I TOTALLY DID NOT EXPECT THIS LINE OF CODE TO BE REACHED");
			return null;
		}
		previousFramePacket = framePacket;
	
		BufferedImage bufferedImage = new BufferedImage(framePacket.getFrameDimensions().width, framePacket.getFrameDimensions().height,
				BufferedImage.TYPE_INT_RGB);
		bufferedImage.setRGB(0, 0, framePacket.getFrameDimensions().width, framePacket.getFrameDimensions().height, framePacket.getData(), 0,
				framePacket.getFrameDimensions().width);
	
		previousImage = bufferedImage;
	
		return bufferedImage;
	}

	private void decodeFramePacketUsingRunTimeEncoding(DecompressionFramePacket packet) {
		packet.decodedData = new int[packet.getFrameSize()];
	
		int inCursor = 0;
		int outCursor = 0;
	
		int blockSize = 0;
	
		int rgb = 0xFF000000;
	
		while (inCursor < packet.encodedData.length - 3 && outCursor < packet.getFrameSize()) {
			if (packet.encodedData[inCursor] == STREAK_OF_SAME_AS_LAST_TIME_BLOCKS_CONSTANT) 
			{
				inCursor++;
	
				int count = (packet.encodedData[inCursor] & 0xFF);
				inCursor++;
	
				int size = count * MAX_BLOCK_LENGTH;
				if (size > packet.decodedData.length) {
					size = packet.decodedData.length;
				}
	
				for (int loop = 0; loop < (MAX_BLOCK_LENGTH * count); loop++) {
					packet.decodedData[outCursor] = packet.previousData[outCursor];
					outCursor++;
					if (outCursor >= packet.decodedData.length) {
						break;
					}
				}
	
			} 
			else if (packet.encodedData[inCursor] < 0) // data is uncompressed
			{
				blockSize = packet.encodedData[inCursor] & 0x7F;
				inCursor++;
	
				for (int loop = 0; loop < blockSize; loop++) {
					rgb = ((packet.encodedData[inCursor] & 0xFF) << 16)
							| ((packet.encodedData[inCursor + 1] & 0xFF) << 8)
							| (packet.encodedData[inCursor + 2] & 0xFF) | ALPHA;
					if (rgb == ALPHA) {
						rgb = packet.previousData[outCursor];
					}
					inCursor += 3;
					packet.decodedData[outCursor] = rgb;
					outCursor++;
					if (outCursor >= packet.decodedData.length) {
						break;
					}
				}
			} 
			else 
			{
				blockSize = packet.encodedData[inCursor];
				inCursor++;
				rgb = ((packet.encodedData[inCursor] & 0xFF) << 16)
						| ((packet.encodedData[inCursor + 1] & 0xFF) << 8)
						| (packet.encodedData[inCursor + 2] & 0xFF) | ALPHA;
	
				boolean transparent = false;
				if (rgb == ALPHA) {
					transparent = true;
				}
				inCursor += 3;
				for (int loop = 0; loop < blockSize && outCursor < packet.getFrameSize(); loop++) {
					if (transparent) {
						packet.decodedData[outCursor] = packet.previousData[outCursor];
					} else {
						packet.decodedData[outCursor] = rgb;
					}
					outCursor++;
				}
			}
		}
		
		logger.debug(String.format("Ending inCursor: %d/%d and outCursor: %d/%d",inCursor,packet.encodedData.length-1,outCursor,packet.decodedData.length-1));
		
		//Many times, if we are on the last couple of bytes, we don't read the last 2 bytes that look something like
		// [..., -1, 62]  and these are ignored because the loop stops if we are past the 3rd to last byte
		// So, the best way to fix this is just assume if we haven't filled the expected size, copy all the stuff from the end
		for(;outCursor<packet.decodedData.length && outCursor<packet.previousData.length;outCursor++)
		{
			packet.decodedData[outCursor] = packet.previousData[outCursor];
		}
		
		packet.setResult(DecompressionFramePacket.CHANGES_THIS_FRAME);
	}

	private byte[] readCompressedData(InputStream inputStream) throws IOException, ReachedEndOfCapFileException {
	
		int compressedFrameSize = readCompressedFrameSize(inputStream);
	
		if (compressedFrameSize < 0 && compressedFrameSize != NORMAL_END_OF_CAP_FILE_COMPRESSED_FRAME_SIZE)
		{
			logger.error("Frame size was unexpectedly negative "+compressedFrameSize);
			throw new ReachedEndOfCapFileException("Frame size was unexpectedly negative "+compressedFrameSize+", so just assuming end of file"); 
		}
		
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

	private int readCompressedFrameSize(InputStream inputStream) throws IOException, ReachedEndOfCapFileException 
	{
		int zSize = 0;
		for (int i = 0 ;i< 4;i++)		//4 bytes to an int
		{
			zSize = zSize << 8;	//done here to effectively have the bit shifting only done 3 times
			int readBuffer = getNextByte(inputStream);
			zSize += readBuffer;
		}
	
		logger.trace("Zipped Frame size:"+zSize);
		return zSize;
	}

	private int getNextByte(InputStream inputStream) throws IOException, ReachedEndOfCapFileException {
		int readBuffer;
		readBuffer = inputStream.read();
		if (readBuffer == -1)
		{
			throw new ReachedEndOfCapFileException("Normal End of File (I think)");
		}
		return readBuffer;
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
	
	private Date makeNewFrameTimeStamp(int timeOffset) 
	{
		if (this.firstFrameTimeStamp == -1)
		{
			this.firstFrameTimeStamp = timeOffset;
			return this.capFileStartTime;
		}
		return new Date(this.capFileStartTime.getTime() + (timeOffset - this.firstFrameTimeStamp));
	}
	
}
