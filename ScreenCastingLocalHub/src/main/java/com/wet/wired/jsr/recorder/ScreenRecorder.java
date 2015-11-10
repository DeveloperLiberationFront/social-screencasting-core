/*
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

package com.wet.wired.jsr.recorder;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

public abstract class ScreenRecorder implements Runnable {

	private static final int FRAME_RATE_LIMITER = 190;
	private static Logger logger = Logger.getLogger(ScreenRecorder.class.getName());
	private DateFormat formatterForFrames = new SimpleDateFormat("DDDyykkmmssSSS");
	private List <ByteArrayOutputStream> imageList=new ArrayList<ByteArrayOutputStream>();
	private Rectangle recordArea;

	private int frameSize;

	private boolean recording = false;
	private boolean running = false;

	private ScreenRecorderListener listener;

	private File outputFolder;
	private boolean isPaused;

	public ScreenRecorder(File outputFolder, ScreenRecorderListener listener)
	{

		this.listener = listener;
		this.outputFolder = outputFolder;
	}

	public void triggerRecordingStop()
	{
		recording = false;

	}

	@Override
	public void run()
	{
		recording = true;
		running = true;
		long lastFrameTime = 0;
		long time = 0;

		frameSize = recordArea.width * recordArea.height;

		while (recording)
		{
			time = System.currentTimeMillis();
			while (time - lastFrameTime < FRAME_RATE_LIMITER)
			{
				try
				{
					Thread.sleep(15);
				}
				catch (Exception e)
				{
					//don't really care here
				}
				time = System.currentTimeMillis();
			}
			lastFrameTime = time;
			
			if (!isPaused)
			{
				try
				{
					recordFrame();
				}
				catch (Exception e)
				{
					logger.error("Problem in main loop", e);
					break;
				}
			}
		}

		running = false;
		recording = false;

		listener.recordingStopped();
	}

	public abstract Rectangle initialiseScreenCapture();

	public abstract BufferedImage captureScreen(Rectangle areaToRecord);

	public void recordFrame() throws IOException
	{
		BufferedImage bImage = captureScreen(recordArea);

		writeImageToDisk(bImage);
		//writeImageToRAM(bImage);
		listener.frameRecorded(true);
	}

	protected void writeImageToDisk(RenderedImage bImage) throws IOException
	{
		
		ImageIO.write(bImage, "jpg", makeFile(new Date()));
	}
	protected void writeImageToRAM(BufferedImage bImage) throws IOException
	{		
		ImageIO.write(bImage, "jpg", makeStream());
		logger.info("Current no. of images "+imageList.size());
	}
	
	protected File makeFile(Date date)
	{
		// round to nearest 10th of a second
		date.setTime((date.getTime() / 100) * 100); 

		return new File(outputFolder, "frame." + formatterForFrames.format(date) + ".jpg");
	}
	protected ByteArrayOutputStream makeStream()
	{
		ByteArrayOutputStream stream= new ByteArrayOutputStream (131072);
		imageList.add(stream);
		return stream;
	}
	public void startRecording()
	{
		recordArea = initialiseScreenCapture();

		if (recordArea == null)
		{
			return;
		}

		new Thread(this, "Screen Recorder").start();
	}

	public void stopRecording()
	{
		triggerRecordingStop();

		int count = 0;
		while (running == true && count < 10)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (Exception e)
			{
				//don't care about interrupted
			}
			count++;
		}
	}

	public boolean isRecording()
	{
		return recording;
	}

	public int getFrameSize()
	{
		return frameSize;
	}

	public void pause(boolean b)
	{
		isPaused = b;
	}

}
