package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import edu.ncsu.lubick.localHub.videoPostProduction.gif.ImagesToGifOutput;

public class ImagesToMiniGifOutput extends ImagesToGifOutput {

	public ImagesToMiniGifOutput()
	{
		super(PostProductionHandler.getIntermediateFolderLocation());
	}
	
	@Override
	protected BufferedImage readInImage(File f) throws IOException
	{
		BufferedImage bigImage = super.readInImage(f);
		BufferedImage shrunkImage = ThumbnailGenerator.shrinkImage(bigImage);
		return shrunkImage;
	}
	
	@Override
	protected File makeGifFile(String fileNameMinusExtension) throws IOException
	{
		File newGifFile = new File(fileNameMinusExtension + ".mini." + GIF_EXTENSION);
		cleanUpForFile(newGifFile);
		
		return newGifFile; 
	}

}
