package edu.ncsu.lubick.localHub.videoPostProduction.outputs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.AbstractImagesToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedTextAndKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedTextMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.KeypressAnimationMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.ShortcutsToKeyCodesConverter;
import edu.ncsu.lubick.util.FileUtilities;

public class PreAnimationImagesToBrowserAnimatedPackage extends AbstractImagesToMediaOutput implements PreAnimationImagesToMediaOutput
{

	private static Logger logger = Logger.getLogger(PreAnimationImagesToBrowserAnimatedPackage.class.getName());
	private ShortcutsToKeyCodesConverter keyCodeReader = new ShortcutsToKeyCodesConverter();
	private List<KeypressAnimationMaker> animationSources = new ArrayList<>();
	private Dimension size;
	private File newDir;

	public PreAnimationImagesToBrowserAnimatedPackage()
	{
		super(new File(PostProductionHandler.getIntermediateFolderLocation()));
	}

	@Override
	public String getMediaTypeInfo()
	{
		return "browser/package";
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

	@Override
	public File combineImageFilesToMakeMedia(String folderName, ToolUsage toolUsage) throws MediaEncodingException
	{
		this.newDir = super.makeDirectoryIfClear(folderName);
		try
		{
			BufferedImage lastFrame = this.copyImagesToFolderAndReturnLast();
			int frameCounter = duplicateLastFrame5Times(lastFrame);
			add5FramesOfBlack(frameCounter);

			this.lazyLoadAnimationSources();
			this.createAnimationImagesForToolStream(toolUsage);
			return newDir;
		}
		catch (IOException e)
		{
			throw new MediaEncodingException(e);
		}
	}

	private void add5FramesOfBlack(int frameCounter) throws IOException
	{
		BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();

		g.setBackground(Color.black);
		
		for(int i = frameCounter;i<frameCounter+5;i++)
		{
			String newFileName = "frame" + FileUtilities.padIntTo4Digits(i) + "." + PostProductionHandler.INTERMEDIATE_FILE_FORMAT;
			File newFile = new File(newDir, newFileName);
			ImageIO.write(image, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, newFile);
		}
		
	}

	private int duplicateLastFrame5Times(BufferedImage lastFrame) throws IOException
	{
		int numFramesSoFar = getImageFilesToAnimate().length;
		this.size = new Dimension(lastFrame.getWidth(), lastFrame.getHeight());
		for(int i = numFramesSoFar;i<numFramesSoFar+5;i++)
		{
			String newFileName = "frame" + FileUtilities.padIntTo4Digits(i) + "." + PostProductionHandler.INTERMEDIATE_FILE_FORMAT;
			File newFile = new File(newDir, newFileName);
			ImageIO.write(lastFrame, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, newFile);
		}
		
		return numFramesSoFar + 5;
		
	}

	private void lazyLoadAnimationSources() throws IOException
	{
		if (animationSources.size() == 0)
		{
			animationSources.add(new AnimatedKeyboardMaker());
			animationSources.add(new AnimatedTextAndKeyboardMaker());
			animationSources.add(new AnimatedTextMaker());
		}

	}

	private void createAnimationImagesForToolStream(ToolUsage toolUsage) throws IOException
	{
		if (toolUsage == null || toolUsage.getToolKeyPresses().equals("MENU"))
		{
			return; // no animation for menus
		}

		for (KeypressAnimationMaker animationSource : animationSources)
		{
			BufferedImage unactivatedAnimation = animationSource.makeUnactivatedAnimation();
			int[] keyCodes = keyCodeReader.convert(toolUsage.getToolKeyPresses());

			BufferedImage activatedAnimation = animationSource.makeNewAnimationForKeyPresses(keyCodes, toolUsage.getToolKeyPresses());

			String animationPrefix = animationSource.getAnimationTypeName();
			File unactivatedAnimationFile = new File(newDir, animationPrefix + "_un.png");
			File activatedAnimationFile = new File(newDir, animationPrefix + ".png");
			ImageIO.write(unactivatedAnimation, "png", unactivatedAnimationFile);
			ImageIO.write(activatedAnimation, "png", activatedAnimationFile);
		}

	}

	private BufferedImage copyImagesToFolderAndReturnLast() throws IOException
	{
		File[] imageFilesToAnimate = getImageFilesToAnimate();
		
		if (imageFilesToAnimate == null  || imageFilesToAnimate.length == 0)
		{
			throw new IOException("Empty Temporary Folder");
		}
		
		for (File originalImageFile : imageFilesToAnimate)
		{
			String destinationFileNumberAndExtension = originalImageFile.getName().substring("temp".length());
			String destinationFileName = "frame" + destinationFileNumberAndExtension;
			File destination = new File(newDir, destinationFileName);
			Files.copy(originalImageFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		BufferedImage lastFrame = ImageIO.read(imageFilesToAnimate[imageFilesToAnimate.length-1]);
		return lastFrame;
	}

}
