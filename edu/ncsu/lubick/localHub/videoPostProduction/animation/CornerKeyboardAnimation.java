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

public class CornerKeyboardAnimation implements PostProductionAnimationStrategy 
{

	private static Logger logger = Logger.getLogger(CornerKeyboardAnimation.class.getName());
	private static final int TIME_FOR_ACTIVATED_ANIMATION = 2;	//in seconds
	
	private File scratchDir;
	private int frameRate;
	private int runUpTime;
	
	private KeypressAnimationMaker animationSource = null;  //lazy load
	private ShortcutsToKeyCodesConverter keyCodeReader = new ShortcutsToKeyCodesConverter();
	
	private ImageDiskWritingStrategy diskWriter = null;

	public CornerKeyboardAnimation(String scratchDirPath, int frameRate, int runUpTime)
	{
		this.scratchDir = new File(scratchDirPath);
		if (!this.scratchDir.exists() || !this.scratchDir.isDirectory())
		{
			logger.fatal("Could not find the scratchDir "+scratchDir.getAbsolutePath());
		}
		this.frameRate = frameRate;
		this.runUpTime = runUpTime;
		
		diskWriter = new ThreadedImageDiskWritingStrategy(scratchDirPath, false);
		
	}

	@Override
	public void addAnimationToImagesInScratchFolderForToolStream(ToolUsage toolUsage) throws IOException
	{
		if (toolUsage.getToolKeyPresses().equals("MENU"))
		{
			return;	//no animation for menus
		}
		
		if (animationSource == null)
		{
			animationSource = new AnimatedKeyboardMaker();
		}
		diskWriter.resetWithOutClearingFolder();

		File[] imagesToAddAnimationTo = scratchDir.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname)
			{
				return pathname.getName().endsWith(PostProductionHandler.INTERMEDIATE_FILE_FORMAT);
			}
		});
		Arrays.sort(imagesToAddAnimationTo);
		
		//Make these once and reuse them below
		BufferedImage unactivatedAnimation = animationSource.makeUnactivatedAnimation();
		int[] keyCodes = keyCodeReader.convert(toolUsage.getToolKeyPresses());
		BufferedImage activatedAnimation = animationSource.makeAnimationForKeyCodes(keyCodes);
		
		
		int i = 0;
		for(;i<frameRate * runUpTime;i++)
		{
			File f = imagesToAddAnimationTo[i];	
			addAnimationToImageAndSaveToDisk(unactivatedAnimation, f);
		}
		
		for(int j = 0;j<frameRate * TIME_FOR_ACTIVATED_ANIMATION && (i + j) < imagesToAddAnimationTo.length ;j++)
		{
			File f = imagesToAddAnimationTo[i+j];
			addAnimationToImageAndSaveToDisk(activatedAnimation, f);
		}
		
		diskWriter.waitUntilDoneWriting();
	}

	private void addAnimationToImageAndSaveToDisk(BufferedImage animation, File sourceFile) throws IOException
	{
		BufferedImage frameImage = ImageIO.read(sourceFile);
		Graphics2D g = frameImage.createGraphics();
		

		int x = frameImage.getWidth() - animation.getWidth();
		int y = frameImage.getHeight() - animation.getHeight();
		g.drawImage(animation, x, y, animation.getWidth(), animation.getHeight(), null);
		
		diskWriter.writeImageToDisk(frameImage, sourceFile);
	}

}
