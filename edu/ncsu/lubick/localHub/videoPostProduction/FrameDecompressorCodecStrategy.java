package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;


public interface FrameDecompressorCodecStrategy
{

	DecompressionFramePacket decodeFramePacket(DecompressionFramePacket framePacket);

	BufferedImage createBufferedImageFromDecompressedFramePacket(DecompressionFramePacket framePacket);

}
