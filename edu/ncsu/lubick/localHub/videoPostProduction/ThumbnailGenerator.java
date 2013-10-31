package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

public class ThumbnailGenerator extends AbstractImagesToMediaOutput 
{
	
	public static final String THUMBNAIL_EXTENSION = "png";

	public ThumbnailGenerator()
	{
		super(new File(PostProductionHandler.getIntermediateFolderLocation()));
	}

	private static Logger logger = Logger.getLogger(ThumbnailGenerator.class.getName());

	@Override
	public File combineImageFilesToMakeMedia(String fileNameMinusExtension) throws IOException
	{
		File newGifFile = makePngFile(fileNameMinusExtension);
		
		File[] imageFiles = getImageFilesToAnimate();
		if (imageFiles.length == 0)
		{
			throw new IOException("Cannot make a thumbnail from nothing");
		}
		File fileToCopy = imageFiles[imageFiles.length/2];
		
		BufferedImage imageToResize = ImageIO.read(fileToCopy);
		int oldWidth = imageToResize.getWidth();
		int newWidth = oldWidth/3;
		
		int oldHeight = imageToResize.getHeight();
		int newHeight = oldHeight/3;
		
		BufferedImage image = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		
		Graphics g = image.getGraphics();
		g.drawImage(imageToResize, 0, 0, newWidth, newHeight, 0, 0, oldWidth, oldHeight, null);
		
		return newGifFile;
	}

	private File makePngFile(String fileNameMinusExtension) throws IOException
	{
		File newPngFile = new File(fileNameMinusExtension + "." + THUMBNAIL_EXTENSION);
		cleanUpForFile(newPngFile);
		
		return newPngFile; 
	}

	@Override
	public String getMediaTypeInfo()
	{
		return "image/png";
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
