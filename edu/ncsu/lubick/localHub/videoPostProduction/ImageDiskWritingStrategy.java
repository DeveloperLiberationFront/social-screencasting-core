package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface ImageDiskWritingStrategy {

	void waitUntilDoneWriting();

	void writeImageToDisk(BufferedImage tempImage) throws IOException;

	void reset();
}
