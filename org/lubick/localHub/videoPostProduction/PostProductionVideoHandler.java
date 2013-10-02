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

	
	public FramePacket unpackNextFrame(InputStream inputStream) throws IOException 
	{
		
		FramePacket frame = new FramePacket(currentFrameRect.width * currentFrameRect.height, previousFramePacket);
		//frame.nextFrame();

		
		int type = readAndSetFrameTimeStamp(frame, inputStream);

		if (type == NO_CHANGES_THIS_FRAME) {
			frame.setResult(NO_CHANGES_THIS_FRAME);
			return frame;
		}

		int i;
		try (ByteArrayOutputStream bO = new ByteArrayOutputStream();) {
			i = inputStream.read();
			int zSize = i;
			zSize = zSize << 8;
			i = inputStream.read();
			zSize += i;
			zSize = zSize << 8;
			i = inputStream.read();
			zSize += i;
			zSize = zSize << 8;
			i = inputStream.read();
			zSize += i;

			logger.trace("Zipped Frame size:"+zSize);

			byte[] zData = new byte[zSize];
			int readCursor = 0;
			int sizeRead = 0;

			while (sizeRead > -1) {
				readCursor += sizeRead;
				if (readCursor >= zSize) {
					break;
				}

				sizeRead = inputStream.read(zData, readCursor, zSize - readCursor);
			}

//			if (ScreenRecordingModule.useCompression)
//			{
				ByteArrayInputStream bI = new ByteArrayInputStream(zData);
				
				//GZIPInputStream zI = new GZIPInputStream(bI);
				InflaterInputStream zI = new InflaterInputStream(bI);
				
				byte[] buffer = new byte[1000];
				sizeRead = zI.read(buffer);

				while (sizeRead > -1) {
					bO.write(buffer, 0, sizeRead);
					bO.flush();

					sizeRead = zI.read(buffer);
				}
				bO.flush();
//			}
//			else 
//			{
//				ByteArrayInputStream bI = new ByteArrayInputStream(zData);
//
//				byte[] buffer = new byte[1000];
//				sizeRead = bI.read(buffer);
//
//				while (sizeRead > -1) {
//					bO.write(buffer, 0, sizeRead);
//					bO.flush();
//
//					sizeRead = bI.read(buffer);
//				}
//				bO.flush();
//			}
			frame.packed = bO.toByteArray();
		} 
		catch (IOException e) 
		{
			logger.error("Problem unpacking ",e);
			frame.setResult(0);
			return frame;
		}


		

		runLengthDecode();

		return frame;
	}

	/*
	 * The first four bytes from the data stream 
	 */
	private int readAndSetFrameTimeStamp(FramePacket frame,InputStream inputStream) throws IOException {
		int i = inputStream.read();
		int time = i;
		time = time << 8;
		i = inputStream.read();
		time += i;
		time = time << 8;
		i = inputStream.read();
		time += i;
		time = time << 8;
		i = inputStream.read();
		time += i;

		frame.setFrameTimeStamp(new Date(time));
		logger.trace("ft:"+time);

		byte type = (byte) inputStream.read();
		logger.trace("Packed Code:"+type);
		//Convert the type to a positive int.  It shouldn't be otherwise, but just in case
		return (type & 0xFF);
	}

}
