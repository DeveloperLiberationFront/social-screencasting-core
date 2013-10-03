package org.lubick.localHub.videoPostProduction;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.InflaterInputStream;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.lubick.localHub.FileUtilities;
import org.lubick.localHub.ToolStream.ToolUsage;

/* Some parts of this (the decoding aspect) have the following license:
 * 
 * This software is OSI Certified Open Source Software
 * 
 * The MIT License (MIT)
 * Copyright 2000-2001 by Wet-Wired.com Ltd., Portsmouth England
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 */
public class PostProductionVideoHandler 
{
	private static Logger logger = Logger.getLogger(PostProductionVideoHandler.class.getName());

	public static final String EXPECTED_FILE_EXTENSION = ".cap";
	
	
	private File currentCapFile;

	private Date capFileStartTime;

	private Rectangle currentFrameRect;

	FramePacket previousFramePacket = null;

	private int firstFrameTimeStamp;

	private boolean reachedEOF;
	
	private int currentTempImageNumber = -1;
	
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
		try(FileInputStream inputStream = new FileInputStream(currentCapFile);) 
		{
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
		currentTempImageNumber = -1;
		
		for(int i = 0;i<20;i++)
		{
			writeImageToDisk(readInFrameImage(inputStream));
		}
		
		return null;
	}

	
	private void writeImageToDisk(BufferedImage readFrame) throws IOException {
		File f = new File(getNextFileName());
		if (!f.createNewFile())
		{
			logger.debug("The image file already exists, going to overwrite");
		}
		ImageIO.write(readFrame, "png", f);
	}

	
	
	private String getNextFileName() 
	{
		currentTempImageNumber++;
		return "./Scratch/temp"+FileUtilities.padIntTo4Digits(currentTempImageNumber)+".png";
	}
	
	BufferedImage previousImage = null;
	public BufferedImage readInFrameImage(InputStream inputStream) throws IOException 
	{

		FramePacket framePacket = unpackNextFrame(inputStream);
		
		logger.debug("read in frame "+framePacket);
		
		//Date frameTime = frame.getFrameTimeStamp();
		
		int result = framePacket.getResult();
		if (result == FramePacket.NO_CHANGES_THIS_FRAME) {
			return previousImage;
		} else if (result == FramePacket.REACHED_END) {
			reachedEOF = true;
			return previousImage;
		}
		previousFramePacket = framePacket;

		BufferedImage bufferedImage = new BufferedImage(currentFrameRect.width, currentFrameRect.height,
				BufferedImage.TYPE_INT_RGB);
		bufferedImage.setRGB(0, 0, currentFrameRect.width, currentFrameRect.height, framePacket.getData(), 0,
				currentFrameRect.width);
		
		previousImage = bufferedImage;

		return bufferedImage;
	}


	public FramePacket unpackNextFrame(InputStream inputStream) throws IOException 
	{

		FramePacket frame = new FramePacket(currentFrameRect.width * currentFrameRect.height, previousFramePacket);

		Date timeStamp = readInFrameTimeStamp(inputStream);

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
	

}
