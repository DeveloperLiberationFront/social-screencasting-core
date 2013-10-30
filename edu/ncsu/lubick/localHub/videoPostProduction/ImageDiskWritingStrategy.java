package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public interface ImageDiskWritingStrategy {

	void waitUntilDoneWriting();

	void writeImageToDisk(BufferedImage image) throws IOException;

	void reset();

	void writeImageToDisk(BufferedImage image, File outputFile) throws IOException;
}
