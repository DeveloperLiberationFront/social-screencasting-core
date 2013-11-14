package edu.ncsu.lubick.localHub.videoPostProduction.outputs;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;

public class ImagesWithAnimationToMiniGifOutput extends ImagesWithAnimationToGifOutput
{
	public static final String MINI_GIF_EXTENSION = "mini." + GIF_EXTENSION;

	@Override
	protected BufferedImage readInImage(File f) throws IOException
	{
		BufferedImage bigImage = super.readInImage(f);
		BufferedImage shrunkImage = shrinkImageBGR(bigImage);
		return shrunkImage;
	}

	@Override
	protected File makeGifFile(String fileNameMinusExtension) throws MediaEncodingException
	{
		File newGifFile = new File(fileNameMinusExtension + "." + MINI_GIF_EXTENSION);
		cleanUpForFile(newGifFile);

		return newGifFile;
	}

	@Override
	public String getMediaTypeInfo()
	{
		return super.getMediaTypeInfo() + " (mini)";
	}

	public static BufferedImage shrinkImageBGR(BufferedImage imageToResize)
	{
		int oldWidth = imageToResize.getWidth();
		int newWidth = oldWidth / 3;

		int oldHeight = imageToResize.getHeight();
		int newHeight = oldHeight / 3;

		BufferedImage shrunkImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);

		Graphics g = shrunkImage.getGraphics();
		g.drawImage(imageToResize, 0, 0, newWidth, newHeight, 0, 0, oldWidth, oldHeight, null);
		return shrunkImage;
	}
}
