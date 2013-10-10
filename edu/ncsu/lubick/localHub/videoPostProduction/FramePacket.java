package edu.ncsu.lubick.localHub.videoPostProduction;

import java.util.Date;

import org.apache.log4j.Logger;

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
public class FramePacket //TODO combine with the FramePacket in ScreenCastingModule
{
	
	private static Logger logger = Logger.getLogger(FramePacket.class.getName());
	
	public static final int NO_CHANGES_THIS_FRAME = 0;
	public static final int CHANGES_THIS_FRAME = 1;
	public static final int REACHED_END = -1;
	public static final int ALPHA = 0xFF000000;
	
	private static final int MAX_BLOCK_LENGTH = 126;

	private static final byte STREAK_OF_SAME_AS_LAST_TIME_BLOCKS_CONSTANT = (byte) 0xFF;
	
	
	private int[] previousData = new int[1];	//to avoid null pointers
	private int result;
	private Date frameTimeStamp;
	private byte[] encodedData = new byte[1];
	private int frameSize;
	private int[] decodedData = new int[1];

	public int getFrameSize() {
		return frameSize;
	}

	public FramePacket(int expectedSize, FramePacket previousFramePacket) {
		this.frameSize = expectedSize;
		if (previousFramePacket == null)
		{
			previousData = new int[frameSize];
		}
		else
		{
			previousData = previousFramePacket.decodedData;
		}
	}



	public int[] getData() {
		return decodedData;
	}

	public int getResult() {
		return result;
	}

	public Date getFrameTimeStamp() {
		return frameTimeStamp;
	}

	public void setFrameTimeStamp(Date date) {
		this.frameTimeStamp = date;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public byte[] getEncodedData() {
		return encodedData;
	}

	public void setEncodedData(byte[] packed) {
		this.encodedData = packed;
	}

	public void runLengthDecode() {
		this.decodedData = new int[this.getFrameSize()];

		int inCursor = 0;
		int outCursor = 0;

		int blockSize = 0;

		int rgb = 0xFF000000;

		while (inCursor < this.encodedData.length - 3 && outCursor < this.getFrameSize()) {
			if (this.encodedData[inCursor] == STREAK_OF_SAME_AS_LAST_TIME_BLOCKS_CONSTANT) 
			{
				inCursor++;

				int count = (this.encodedData[inCursor] & 0xFF);
				inCursor++;

				int size = count * MAX_BLOCK_LENGTH;
				if (size > this.decodedData.length) {
					size = this.decodedData.length;
				}

				for (int loop = 0; loop < (MAX_BLOCK_LENGTH * count); loop++) {
					this.decodedData[outCursor] = this.previousData[outCursor];
					outCursor++;
					if (outCursor >= this.decodedData.length) {
						break;
					}
				}

			} 
			else if (this.encodedData[inCursor] < 0) // data is uncompressed
			{
				blockSize = this.encodedData[inCursor] & 0x7F;
				inCursor++;

				for (int loop = 0; loop < blockSize; loop++) {
					rgb = ((this.encodedData[inCursor] & 0xFF) << 16)
							| ((this.encodedData[inCursor + 1] & 0xFF) << 8)
							| (this.encodedData[inCursor + 2] & 0xFF) | ALPHA;
					if (rgb == ALPHA) {
						rgb = this.previousData[outCursor];
					}
					inCursor += 3;
					this.decodedData[outCursor] = rgb;
					outCursor++;
					if (outCursor >= this.decodedData.length) {
						break;
					}
				}
			} 
			else 
			{
				blockSize = this.encodedData[inCursor];
				inCursor++;
				rgb = ((this.encodedData[inCursor] & 0xFF) << 16)
						| ((this.encodedData[inCursor + 1] & 0xFF) << 8)
						| (this.encodedData[inCursor + 2] & 0xFF) | ALPHA;

				boolean transparent = false;
				if (rgb == ALPHA) {
					transparent = true;
				}
				inCursor += 3;
				for (int loop = 0; loop < blockSize && outCursor < getFrameSize(); loop++) {
					if (transparent) {
						this.decodedData[outCursor] = this.previousData[outCursor];
					} else {
						this.decodedData[outCursor] = rgb;
					}
					outCursor++;
				}
			}
		}
		
		logger.debug("Ending inCursor: "+inCursor+" outCursor: "+outCursor);
		
		//TODO this is a hotfix for the bottom of the screen going dark.  I think there is some 
		//deeper problem, but this fixes it for now.
		//for(;outCursor<getFrameSize();outCursor++)
		//{
		//	this.decodedData[outCursor] = this.previousData[outCursor];
		//}
		this.result = FramePacket.CHANGES_THIS_FRAME;
	}
	
	@Override
	public String toString() {
		return "FramePacket [frameSize=" + frameSize + ", result=" + result
				+ ", frameTimeStamp=" + frameTimeStamp + ", previousDataLength="
				+ previousData.length + ", encodedDataLength="
				+ encodedData.length + ", decodedDataLength="
				+ decodedData.length + "]";
	}

}
