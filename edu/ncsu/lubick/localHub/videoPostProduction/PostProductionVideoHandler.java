package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

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

	public static final boolean DELETE_IMAGES_AFTER_USE = true;

	private static final int FRAME_RATE = 5;
	
	private static final int RUN_UP_TIME = 5;
	
	private static final int TOOL_DEMO_TIME = 15;

	private File currentCapFile;

	private Date capFileStartTime;

	

	private Queue<OverloadFile> queueOfOverloadFiles = new LinkedList<>();
	
	private FrameDecompressor decompressor = new FrameDecompressor();
	private ImageDiskWritingStrategy imageWriter = new ThreadedImageDiskWritingStrategy("./Scratch/", DELETE_IMAGES_AFTER_USE);
	//private ImageDiskWritingStrategy imageWriter = new BlockingImageDiskWritingStrategy("./Scratch/", DELETE_IMAGES_AFTER_USE);

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
		decompressor.setFrameZeroTime(capFileStartTime);
	}

	public File extractVideoForToolUsage(ToolUsage specificToolUse)
	{
		if (this.currentCapFile== null || this.capFileStartTime == null)
		{
			logger.error("PostProductionVideo object needed to have a file to load and a start time");
			return null;
		}
		
		Date timeToLookFor = new Date(specificToolUse.getTimeStamp().getTime() - RUN_UP_TIME *1000);
		if (timeToLookFor.before(capFileStartTime))
		{
			timeToLookFor = capFileStartTime;
		}
		
			
		File retVal = null;
		try(FileInputStream inputStream = new FileInputStream(currentCapFile);) 
		{
			decompressor.readInFileHeader(inputStream);
			fastFowardStreamToTime(inputStream, timeToLookFor);
			
			File newVideoFile = new File(makeFileNameForToolPlugin(specificToolUse.getPluginName(), specificToolUse.getToolName()));
			retVal = extractDemoVideoToFile(inputStream, newVideoFile);

		} catch (IOException e) {
			logger.error("There was a problem extracting the video",e);
		}
		return retVal;
	}

	private void fastFowardStreamToTime(FileInputStream inputStream, Date timeToLookFor) throws IOException 
	{
		if (timeToLookFor.equals(capFileStartTime))	//no fast forwarding required
		{
			return;
		}
		
		Date currTimeStamp = capFileStartTime;
		
		while (currTimeStamp.before(timeToLookFor))
		{
			decompressor.readInFrameImage(inputStream);
			currTimeStamp = decompressor.getPreviousFrameTimeStamp();
			
		}
	}

	private File extractDemoVideoToFile(InputStream inputStream, File newVideoFile) throws IOException 
	{
		imageWriter.reset();
		for(int i = 0;i<FRAME_RATE * (RUN_UP_TIME + TOOL_DEMO_TIME);i++)
		{
			BufferedImage tempImage = decompressor.readInFrameImage(inputStream);
			if (tempImage == null)
			{
				if (queueOfOverloadFiles.size() == 0)
				{
					logger.error("Ran out of cap files to pull video from.  Truncated with what I've got");
					break;
				}
				inputStream = goToNextFileInQueue(inputStream);
				//call this image a mulligan and go to the top of the loop.
				i--;
				continue;
			}
			imageWriter.writeImageToDisk(tempImage);
		}
		
		imageWriter.waitUntilDoneWriting();
		
		
		if (newVideoFile.exists() && !newVideoFile.delete())
		{
			logger.error("could not establish a temporary video file");
			return null;
		}

		executeEncoding(newVideoFile);

		return newVideoFile;
	}

 
	private void executeEncoding(File newVideoFile) throws IOException 
	{
		//TODO make this more flexible, not hardcoded.  i.e. the user should specify where their ffmpeg is
		
		String executableString = "./src/FFMPEGbin/ffmpeg.exe -r 5 -pix_fmt yuv420p -i ./Scratch/temp%04d.png  -vcodec libx264 "+newVideoFile.getPath();
		
		//Using Runtime.exec() because I couldn't get ProcessBuilder to handle the arguments on 
		//ffempeg well. 
		Process process = Runtime.getRuntime().exec(executableString);
		
		inheritIO(process.getInputStream(), "Normal Output");
		inheritIO(process.getErrorStream(), "Error Output");
		logger.debug("Rendering video");
		try {
			logger.debug("FFMPEG exited with state "+process.waitFor());
		} catch (InterruptedException e) {
			logger.error("There was a problem with ffmpeg",e);
		}
	}

	
	private static void inheritIO(final InputStream src, final String identifer) 
	{
		//used to spy on a process's output, similar to what ProcessBuilder does
		new Thread(new Runnable() {
			public void run() {
				try(Scanner sc = new Scanner(src);) 
				{
					while (sc.hasNextLine()) {
						String string = sc.nextLine();
						logger.trace("From "+identifer+":"+ string);
					}
				} catch (Exception e) {
					logger.error("Problem in stream monitoring",e);
				}

			}
		}).start();
	}

	public static String makeFileNameForToolPlugin(String pluginName, String toolName) 
	{
		if (toolName ==null)
		{
			logger.info("Got a null toolname, recovering with empty string");
			toolName = "";
		}
		return "Scratch/renderedVideos/"+pluginName+createNumberForVideoFile(toolName)+".mkv"; 
	}

	private static int createNumberForVideoFile(String toolName) {
		int retval = toolName.hashCode();
		if (toolName.hashCode() == Integer.MIN_VALUE)
			retval = 0;
		return Math.abs(retval);
	}

	
	/**
	 * Adds an extra file to this handler to use if the wanted toolstream's behavior extends over 
	 * the end of the loaded cap file
	 * @param extraCapFile
	 * @param extraDate
	 */
	public void enqueueOverLoadFile(File extraCapFile, Date extraDate) {
		this.queueOfOverloadFiles.offer(new OverloadFile(extraCapFile, extraDate));
		
	}

	private InputStream goToNextFileInQueue(InputStream oldInputStream) throws IOException 
	{
		oldInputStream.close();
		OverloadFile nextFile = queueOfOverloadFiles.poll();
		FileInputStream inputStream = new FileInputStream(nextFile.file);
		decompressor.readInFileHeader(inputStream);
		
		setCurrentFileStartTime(nextFile.date);

		return inputStream;
	}

	private static class OverloadFile
	{

		private Date date;
		private File file;

		public OverloadFile(File extraCapFile, Date extraDate) {
			this.file = extraCapFile;
			this.date = extraDate;
		}
		
	}
	


}
