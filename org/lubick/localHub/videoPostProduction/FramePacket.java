package org.lubick.localHub.videoPostProduction;

import java.util.Date;

class FramePacket {

	private int[] previousData;
	private int result;
	private Date frameTimeStamp;
	private byte[] encodedData;
	private int frameSize;
	int[] newData;

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
			previousData = previousFramePacket.newData;
		}
	}

	@Deprecated
	private void nextFrame() {
		if (newData != null) {
			previousData = newData;
		}
	}

	public int[] getData() {
		return newData;
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
}
