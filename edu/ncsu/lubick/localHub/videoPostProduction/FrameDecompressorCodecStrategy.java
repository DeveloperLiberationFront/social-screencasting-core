package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;


public interface FrameDecompressorCodecStrategy 
{

	BufferedImage decodeFramePacketToBufferedImage(DecompressionFramePacket framePacket);
	
}
