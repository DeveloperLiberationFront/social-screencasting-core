package edu.ncsu.lubick.localHub.videoPostProduction.outputs;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.videoPostProduction.AbstractImagesToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedTextAndKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedTextMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.KeypressAnimationMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.ShortcutsToKeyCodesConverter;
import edu.ncsu.lubick.util.FileUtilities;

public class FramesToBrowserAnimatedPackage extends AbstractImagesToMediaOutput implements PreAnimationImagesToMediaOutput
{

	private static Logger logger = Logger.getLogger(FramesToBrowserAnimatedPackage.class.getName());
	private ShortcutsToKeyCodesConverter keyCodeReader = new ShortcutsToKeyCodesConverter();
	private List<KeypressAnimationMaker> animationSources = new ArrayList<>();
	private File browserPackageRootDir;
	private File[] sortedFrameFiles;
	private UserManager userManager;


	public FramesToBrowserAnimatedPackage(File sourceOfFrames, UserManager userManager)
	{
		super(sourceOfFrames);
		this.userManager = userManager;
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
	public File combineImageFilesToMakeMedia(ToolUsage toolUsage, int startIndex, int endIndex) throws MediaEncodingException
	{
		String browserPackageRootDirName = FileUtilities.makeLocalFolderNameForBrowserMediaPackage(toolUsage, userManager.getUserEmail());
		this.browserPackageRootDir = super.makeDirectoryIfClear(browserPackageRootDirName);
		try
		{
			copyImagesToFolder(startIndex, endIndex);

			this.lazyLoadAnimationSources();
			this.createAnimationImagesForToolStream(toolUsage);
			return browserPackageRootDir;
		}
		catch (IOException e)
		{
			throw new MediaEncodingException(e);
		}
	}

	private void lazyLoadAnimationSources() throws IOException
	{
		if (animationSources.isEmpty())
		{
			animationSources.add(new AnimatedKeyboardMaker());
			animationSources.add(new AnimatedTextAndKeyboardMaker());
			animationSources.add(new AnimatedTextMaker());
		}

	}

	private void createAnimationImagesForToolStream(ToolUsage toolUsage) throws IOException
	{
		if (toolUsage == null || ToolStream.MENU_KEY_PRESS.equals(toolUsage.getToolKeyPresses()))
		{
			return; // no animation for menus
		}

		for (KeypressAnimationMaker animationSource : animationSources)
		{
			BufferedImage unactivatedAnimation = animationSource.makeUnactivatedAnimation();
			int[] keyCodes = keyCodeReader.convert(toolUsage.getToolKeyPresses());

			BufferedImage activatedAnimation = animationSource.makeNewAnimationForKeyPresses(keyCodes, toolUsage.getToolKeyPresses());

			String animationPrefix = animationSource.getAnimationTypeName();
			File unactivatedAnimationFile = new File(browserPackageRootDir, animationPrefix + "_un.png");
			File activatedAnimationFile = new File(browserPackageRootDir, animationPrefix + ".png");
			ImageIO.write(unactivatedAnimation, "png", unactivatedAnimationFile);
			ImageIO.write(activatedAnimation, "png", activatedAnimationFile);
		}

	}

	private void copyImagesToFolder(int startIndex, int endIndex) throws IOException
	{
	
		if (sortedFrameFiles == null  || sortedFrameFiles.length == 0)
		{
			throw new IOException("Empty Temporary Folder");
		}
		
		int frameIndex = 0;
		for (int i = startIndex; i<sortedFrameFiles.length && i<=endIndex; i++)
		{
			File originalImageFile = sortedFrameFiles[i];
			
			String destinationFileNumberAndExtension = FileUtilities.padIntTo4Digits(frameIndex) + "."+PostProductionHandler.INTERMEDIATE_FILE_FORMAT;
			String destinationFileName = "frame" + destinationFileNumberAndExtension;
			File destination = new File(browserPackageRootDir, destinationFileName);
			Files.copy(originalImageFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
			frameIndex++;
		}

	}

	@Override
	public void setSortedFrames(File[] sortedFrameFiles)
	{
		this.sortedFrameFiles = sortedFrameFiles;
	}

}
