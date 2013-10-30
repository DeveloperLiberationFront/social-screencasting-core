package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.Rectangle;
import java.util.Date;

/*
 * Most of the this class is adapted from software that is under
 * the following license:
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
public class DecompressionFramePacket
{

	public static final int NO_CHANGES_THIS_FRAME = 0;
	public static final int CHANGES_THIS_FRAME = 1;
	public static final int REACHED_END = -1;

	private int result;
	private Date frameTimeStamp;
	// Maybe deprecated
	private int frameSize;

	int[] previousData = new int[1]; // to avoid null pointers
	byte[] encodedData = new byte[1];
	int[] decodedData = new int[1];
	private Rectangle frameDimensions;

	public int getFrameSize()
	{
		return frameSize;
	}

	public DecompressionFramePacket(Rectangle frameDimensions)
	{
		this.frameSize = frameDimensions.width * frameDimensions.height;
		this.frameDimensions = frameDimensions;
		previousData = new int[frameSize];

	}

	public void setPreviousFramePacket(DecompressionFramePacket previousFramePacket)
	{
		if (previousFramePacket == null)
		{
			previousData = new int[frameSize];
		}
		else
		{
			previousData = previousFramePacket.decodedData;
		}
	}

	public int[] getData()
	{
		return decodedData;
	}

	public int getResult()
	{
		return result;
	}

	public Date getFrameTimeStamp()
	{
		return frameTimeStamp;
	}

	public void setFrameTimeStamp(Date date)
	{
		this.frameTimeStamp = date;
	}

	public void setResult(int result)
	{
		this.result = result;
	}

	public byte[] getEncodedData()
	{
		return encodedData;
	}

	public void setEncodedData(byte[] packed)
	{
		this.encodedData = packed;
	}

	@Override
	public String toString()
	{
		return "FramePacket [frameSize=" + frameSize + ", result=" + result
				+ ", frameTimeStamp=" + frameTimeStamp + ", previousDataLength="
				+ previousData.length + ", encodedDataLength="
				+ encodedData.length + ", decodedDataLength="
				+ decodedData.length + "]";
	}

	public Rectangle getFrameDimensions()
	{
		return this.frameDimensions;
	}

}
