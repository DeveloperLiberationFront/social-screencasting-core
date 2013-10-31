package edu.ncsu.lubick.localHub.videoPostProduction.gif;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.junit.experimental.runners.Enclosed;

import edu.ncsu.lubick.localHub.videoPostProduction.ImagesToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class ImagesToGifOutput implements ImagesToMediaOutput {

	private static final String GIF_EXTENSION = "gif";
	private static Logger logger = Logger.getLogger(ImagesToGifOutput.class.getName());
	private File scratchDir;
	
	public ImagesToGifOutput(String scratchDir)
	{
		this.scratchDir = new File(scratchDir);
	}
	
	@Override
	public File combineImageFilesToMakeMedia(String fileNameMinusExtension) throws IOException
	{
		File newGifFile = makeGifFile(fileNameMinusExtension);

		AnimatedGifEncoder encoder = makeGifEncoder(newGifFile);
		
		File[] imageFilesToAnimate = getImageFilesToAnimate();
		
		for(File f:imageFilesToAnimate)
		{
			BufferedImage readInImage = ImageIO.read(f);
			encoder.addFrame(readInImage);
		}
		finishUpAnimation(encoder);
		
		return newGifFile;
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

	private File[] getImageFilesToAnimate()
	{
		File[] imagesToAnimate = scratchDir.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname)
			{
				return pathname.getName().endsWith(PostProductionHandler.INTERMEDIATE_FILE_FORMAT);
			}
		});
		Arrays.sort(imagesToAnimate);
		return imagesToAnimate;
	}

	private AnimatedGifEncoder makeGifEncoder(File newGifFile)
	{
		AnimatedGifEncoder e = new AnimatedGifEncoder();
		e.start(newGifFile);
		e.setDelay(1000/PostProductionHandler.FRAME_RATE);   // 1 frame per sec
		e.setRepeat(AnimatedGifEncoder.REPEAT_INDEFINATELY);
		return e;
	}

	private File makeGifFile(String fileNameMinusExtension) throws IOException
	{
		File newGifFile = new File(fileNameMinusExtension + "." + GIF_EXTENSION);
		if (!newGifFile.getParentFile().mkdirs() && !newGifFile.getParentFile().exists())
		{
			throw new IOException("Could not make the output folder " + newGifFile.getParentFile());
		}

		if (newGifFile.exists() && !newGifFile.delete())
		{
			logger.error("Could not make video file.  Could not delete previous gif " + newGifFile);
			throw new IOException("Could not make video file.  Could not delete previous gif" + newGifFile);
		}
		
		return newGifFile; 
	}

	@Override
	public String getMediaTypeInfo()
	{
		return "Video/gif";
	}

}
