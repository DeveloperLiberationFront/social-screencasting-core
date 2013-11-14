package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.ImageDiskWritingStrategy;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionAnimationStrategy;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.ThreadedImageDiskWritingStrategy;

/**
 * Adds a keyboard animation to the corner of all the images
 * @author KevinLubick
 *
 */
public class CornerKeypressAnimation implements PostProductionAnimationStrategy
{

	private static Logger logger = Logger.getLogger(CornerKeypressAnimation.class.getName());
	private static final int TIME_FOR_ACTIVATED_ANIMATION = 2; // in seconds
	private static final int FRAMES_TO_ACCOUNT_FOR_LAG_TIME = 3;

	private File scratchDir;
	private int frameRate;
	private int runUpTime;

	private KeypressAnimationMaker animationSource = null;
	private ShortcutsToKeyCodesConverter keyCodeReader = new ShortcutsToKeyCodesConverter();

	private ImageDiskWritingStrategy animatedImageOutput = null;

	public CornerKeypressAnimation(String scratchDirPath, int frameRate, int runUpTime, KeypressAnimationMaker animationSource)
	{
		this.scratchDir = new File(scratchDirPath);
		if (!this.scratchDir.exists() || !this.scratchDir.isDirectory())
		{
			logger.fatal("Could not find the scratchDir " + scratchDir.getAbsolutePath());
		}
		this.frameRate = frameRate;
		this.runUpTime = runUpTime;

		animatedImageOutput = new ThreadedImageDiskWritingStrategy(scratchDirPath, PostProductionHandler.DELETE_IMAGES_AFTER_USE);

		this.animationSource = animationSource;
	}
	
	/**
	 *  Makes an object that will add 0 animation to any of the images
	 */
	public CornerKeypressAnimation(String scratchDirPath, int frameRate, int runUpTime)
	{
		this(scratchDirPath,frameRate,runUpTime, null);
		logger.info("Warning: No animation was given to "+this.getClass()+" so, no animations will be added.");
	}

	@Override
	public void addAnimationToImagesInScratchFolderForToolStream(ToolUsage toolUsage) throws IOException
	{
		if (toolUsage.getToolKeyPresses().equals("MENU") || animationSource == null)
		{
			return; // no animation for menus, or if the animationSource happens to be null
		}

		animatedImageOutput.resetWithOutClearingFolder();

		File[] imagesToAddAnimationTo = scratchDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname)
			{
				return pathname.getName().endsWith(PostProductionHandler.INTERMEDIATE_FILE_FORMAT);
			}
		});
		Arrays.sort(imagesToAddAnimationTo);

		// Make these once and reuse them below
		BufferedImage unactivatedAnimation = animationSource.makeUnactivatedAnimation();
		int[] keyCodes = keyCodeReader.convert(toolUsage.getToolKeyPresses());
		
		BufferedImage activatedAnimation = animationSource.makeNewAnimationForKeyPresses(keyCodes, toolUsage.getToolKeyPresses());
		
		int i = 0;
		for (; i < frameRate * runUpTime - FRAMES_TO_ACCOUNT_FOR_LAG_TIME; i++)
		{
			File f = imagesToAddAnimationTo[i];
			addAnimationToImageAndSaveToDisk(unactivatedAnimation, f);
		}

		for (int j = 0; j < frameRate * TIME_FOR_ACTIVATED_ANIMATION && (i + j) < imagesToAddAnimationTo.length; j++)
		{
			File f = imagesToAddAnimationTo[i + j];
			addAnimationToImageAndSaveToDisk(activatedAnimation, f);
		}

		animatedImageOutput.waitUntilDoneWriting();
	}


	private void addAnimationToImageAndSaveToDisk(BufferedImage animation, File sourceFile) throws IOException
	{
		BufferedImage frameImage = ImageIO.read(sourceFile);
		Graphics2D g = frameImage.createGraphics();

		int x = frameImage.getWidth() - animation.getWidth();
		int y = frameImage.getHeight() - animation.getHeight();
		g.drawImage(animation, x, y, animation.getWidth(), animation.getHeight(), null);

		animatedImageOutput.writeImageToDisk(frameImage, sourceFile);
	}

}
