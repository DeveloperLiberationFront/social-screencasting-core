package edu.ncsu.lubick.localHub.videoPostProduction.outputs;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.AbstractImagesToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class ImagesWithAnimationToThumbnailOutput extends AbstractImagesToMediaOutput implements ImagesWithAnimationToMediaOutput
{

	public static final String THUMBNAIL_EXTENSION = "png";

	public ImagesWithAnimationToThumbnailOutput()
	{
		super(new File(PostProductionHandler.getIntermediateFolderLocation()));
	}

	private static Logger logger = Logger.getLogger(ImagesWithAnimationToThumbnailOutput.class.getName());

	@Override
	public File combineImageFilesToMakeMedia(String fileNameMinusExtension) throws MediaEncodingException
	{
		File newGifFile = makePngFile(fileNameMinusExtension);

		File[] imageFiles = getImageFilesToAnimate();
		if (imageFiles.length == 0)
		{
			throw new MediaEncodingException("Cannot make a thumbnail from nothing");
		}
		File fileToCopy = imageFiles[imageFiles.length / 2];

		try
		{
			BufferedImage imageToResize = ImageIO.read(fileToCopy);
			BufferedImage shrunkImage = shrinkImage(imageToResize);

			ImageIO.write(shrunkImage, THUMBNAIL_EXTENSION, newGifFile);

			return newGifFile;
		}
		catch (IOException e)
		{
			throw new MediaEncodingException(e);
		}
	}

	public static BufferedImage shrinkImage(BufferedImage imageToResize)
	{
		int oldWidth = imageToResize.getWidth();
		int newWidth = oldWidth / 3;

		int oldHeight = imageToResize.getHeight();
		int newHeight = oldHeight / 3;

		BufferedImage shrunkImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

		Graphics g = shrunkImage.getGraphics();
		g.drawImage(imageToResize, 0, 0, newWidth, newHeight, 0, 0, oldWidth, oldHeight, null);
		return shrunkImage;
	}

	private File makePngFile(String fileNameMinusExtension) throws MediaEncodingException
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
