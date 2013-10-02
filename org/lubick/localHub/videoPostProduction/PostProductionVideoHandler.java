package org.lubick.localHub.videoPostProduction;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.InflaterInputStream;

import org.apache.log4j.Logger;
import org.lubick.localHub.ToolStream.ToolUsage;


public class PostProductionVideoHandler 
{
	private static Logger logger = Logger.getLogger(PostProductionVideoHandler.class.getName());

	public static final String EXPECTED_FILE_EXTENSION = ".cap";
	private static final int NO_CHANGES_THIS_FRAME = 0;
	private static final int CHANGES_THIS_FRAME = 1;
	private static final int ALPHA = 0xFF000000;
	
	private File currentCapFile;

	private Date capFileStartTime;

	private Rectangle currentFrameRect;

	public void loadFile(File capFile) {
		if (capFile == null)
		{
			logger.error("Recieved null file to load in PostProductionVideoHandler");
			throw new IllegalArgumentException("A capFile cannot be null");
		}
		if (!capFile.getName().endsWith(EXPECTED_FILE_EXTENSION))
		{
			logger.error("Expected cap file to have an extension "+EXPECTED_FILE_EXTENSION +" not like "+capFile.getName());
			this.currentCapFile = null;
		}
		this.currentCapFile = capFile;

	}

	public void setCurrentFileStartTime(Date startTime) {
		if (startTime == null)
		{
			logger.error("Recieved null startTime in PostProductionVideoHandler");
			throw new IllegalArgumentException("The start time cannot be null");
		}
		this.capFileStartTime = startTime;

	}

	public File extractVideoForToolUsage(ToolUsage specificToolUse)
	{
		if (this.currentCapFile== null || this.capFileStartTime == null)
		{
			logger.error("PostProductionVideo object needed to have a file to load and a start time");
			return null;
		}
		File retVal = null;
		try(FileInputStream inputStream = new FileInputStream(currentCapFile);) {
			retVal = extractVideoHelper(inputStream);
		} catch (IOException e) {
			logger.error("There was a problem extracting the video",e);
		}
		return retVal;
	}

	private File extractVideoHelper(InputStream inputStream) throws IOException 
	{
		int width = inputStream.read();
		width = width << 8;
		width += inputStream.read();
		int height = inputStream.read();
		height = height << 8;
		height += inputStream.read();

		this.currentFrameRect = new Rectangle(width, height);
		return null;
	}

	FramePacket previousFramePacket = null;

	private int firstFrameTimeStamp;


	public FramePacket unpackNextFrame(InputStream inputStream) throws IOException 
	{

		FramePacket frame = new FramePacket(currentFrameRect.width * currentFrameRect.height, previousFramePacket);

		Date timeStamp = readInFrameTimeStamp(inputStream);

		frame.setFrameTimeStamp(timeStamp);

		int type = readFrameType(inputStream);

		if (type == NO_CHANGES_THIS_FRAME) {
			frame.setResult(NO_CHANGES_THIS_FRAME);
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
			frame.setResult(NO_CHANGES_THIS_FRAME);
			return frame;
		}

		runLengthDecode(frame);

		return frame;
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

	private Date readInFrameTimeStamp(InputStream inputStream) throws IOException {
		int readBuffer = inputStream.read();
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

	private Date makeNewFrameTimeStamp(int timeOffset) 
	{
		if (this.firstFrameTimeStamp == -1)
		{
			this.firstFrameTimeStamp = timeOffset;
			return this.capFileStartTime;
		}
		return new Date(this.capFileStartTime.getTime() + (timeOffset - this.firstFrameTimeStamp));
	}
	
	//XXX Left off here
	private void runLengthDecode(FramePacket frame) {
		frame.newData = new int[frame.getFrameSize()];

		int inCursor = 0;
		int outCursor = 0;

		int blockSize = 0;

		int rgb = 0xFF000000;

		while (inCursor < frame.packed.length - 3 && outCursor < frame.getFrameSize()) {
			if (frame.packed[inCursor] == -1) {
				inCursor++;

				int count = (frame.packed[inCursor] & 0xFF);
				inCursor++;

				int size = count * 126;
				if (size > frame.newData.length) {
					size = frame.newData.length;
				}

				for (int loop = 0; loop < (126 * count); loop++) {
					frame.newData[outCursor] = frame.previousData[outCursor];
					outCursor++;
					if (outCursor >= frame.newData.length) {
						break;
					}
				}

			} 
			else if (frame.packed[inCursor] < 0) // uncomp
			{
				blockSize = frame.packed[inCursor] & 0x7F;
				inCursor++;

				for (int loop = 0; loop < blockSize; loop++) {
					rgb = ((frame.packed[inCursor] & 0xFF) << 16)
							| ((frame.packed[inCursor + 1] & 0xFF) << 8)
							| (frame.packed[inCursor + 2] & 0xFF) | ALPHA;
					if (rgb == ALPHA) {
						rgb = frame.previousData[outCursor];
					}
					inCursor += 3;
					frame.newData[outCursor] = rgb;
					outCursor++;
					if (outCursor >= frame.newData.length) {
						break;
					}
				}
			} 
			else {
				blockSize = frame.packed[inCursor];
				inCursor++;
				rgb = ((frame.packed[inCursor] & 0xFF) << 16)
						| ((frame.packed[inCursor + 1] & 0xFF) << 8)
						| (frame.packed[inCursor + 2] & 0xFF) | ALPHA;

				boolean transparent = false;
				if (rgb == ALPHA) {
					transparent = true;
				}
				inCursor += 3;
				for (int loop = 0; loop < blockSize; loop++) {
					if (transparent) {
						frame.newData[outCursor] = frame.previousData[outCursor];
					} else {
						frame.newData[outCursor] = rgb;
					}
					outCursor++;
					if (outCursor >= frame.newData.length) {
						break;
					}
				}
			}
		}
		frame.result = outCursor;
	}

}
