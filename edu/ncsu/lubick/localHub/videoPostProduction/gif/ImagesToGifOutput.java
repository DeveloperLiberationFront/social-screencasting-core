package edu.ncsu.lubick.localHub.videoPostProduction.gif;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.AbstractImagesToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class ImagesToGifOutput extends AbstractImagesToMediaOutput {

	public static final String GIF_EXTENSION = "gif";
	private static Logger logger = Logger.getLogger(ImagesToGifOutput.class.getName());

	public ImagesToGifOutput(String scratchDir)
	{
		super(new File(scratchDir));
	}

	@Override
	public File combineImageFilesToMakeMedia(String fileNameMinusExtension) throws IOException
	{
		File newGifFile = makeGifFile(fileNameMinusExtension);

		AnimatedGifEncoder encoder = makeGifEncoder(newGifFile);

		File[] imageFilesToAnimate = getImageFilesToAnimate();

		for (File f : imageFilesToAnimate)
		{
			BufferedImage readInImage = readInImage(f);
			encoder.addFrame(readInImage);
		}
		finishUpAnimation(encoder);

		return newGifFile;
	}

	protected BufferedImage readInImage(File f) throws IOException
	{
		return ImageIO.read(f);
	}

	private void finishUpAnimation(AnimatedGifEncoder encoder)
	{
		addTwoFramesOfBlack(encoder);
		encoder.finish();
	}

	private void addTwoFramesOfBlack(AnimatedGifEncoder encoder)
	{
		Dimension size = encoder.getSize();
		BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();

		g.setBackground(Color.black);

		encoder.addFrame(image);
		encoder.addFrame(image);
	}

	private AnimatedGifEncoder makeGifEncoder(File newGifFile)
	{
		AnimatedGifEncoder e = new AnimatedGifEncoder();
		e.start(newGifFile);
		e.setDelay(1000 / PostProductionHandler.FRAME_RATE); // 1 frame per sec
		e.setRepeat(AnimatedGifEncoder.REPEAT_INDEFINATELY);
		return e;
	}

	protected File makeGifFile(String fileNameMinusExtension) throws IOException
	{
		File newGifFile = new File(fileNameMinusExtension + "." + GIF_EXTENSION);
		cleanUpForFile(newGifFile);

		return newGifFile;
	}

	@Override
	public String getMediaTypeInfo()
	{
		return "Video/gif";
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
