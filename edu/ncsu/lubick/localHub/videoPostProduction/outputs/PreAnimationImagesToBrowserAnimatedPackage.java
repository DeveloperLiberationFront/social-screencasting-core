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

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.AbstractImagesToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedTextAndKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedTextMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.KeypressAnimationMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.ShortcutsToKeyCodesConverter;

public class PreAnimationImagesToBrowserAnimatedPackage extends AbstractImagesToMediaOutput implements PreAnimationImagesToMediaOutput
{

	private static Logger logger = Logger.getLogger(PreAnimationImagesToBrowserAnimatedPackage.class.getName());
	private ShortcutsToKeyCodesConverter keyCodeReader = new ShortcutsToKeyCodesConverter();
	private List<KeypressAnimationMaker> animationSources = new ArrayList<>();

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
	public File combineImageFilesToMakeMedia(String folderName, ToolUsage currentToolStream) throws MediaEncodingException
	{
		File newDir = super.makeDirectoryIfClear(folderName);
		try
		{
			this.copyImagesToFolder(newDir);

			this.lazyLoadAnimationSources();
			this.createAnimationImagesForToolStream(newDir, currentToolStream);
			return newDir;
		}
		catch (IOException e)
		{
			throw new MediaEncodingException(e);
		}
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

	private void createAnimationImagesForToolStream(File newDir, ToolUsage toolUsage) throws IOException
	{
		if (toolUsage.getToolKeyPresses().equals("MENU"))
		{
			return; // no animation for menus
		}

		for (KeypressAnimationMaker animationSource : animationSources)
		{
			BufferedImage unactivatedAnimation = animationSource.makeUnactivatedAnimation();
			int[] keyCodes = keyCodeReader.convert(toolUsage.getToolKeyPresses());
			animationSource.setCurrentKeyPresses(toolUsage.getToolKeyPresses());
			BufferedImage activatedAnimation = animationSource.makeAnimationForKeyCodes(keyCodes);

			String animationPrefix = animationSource.getAnimationName();
			File unactivatedAnimationFile = new File(newDir,animationPrefix+"_un.png");
			File activatedAnimationFile = new File(newDir, animationPrefix+".png");
			ImageIO.write(unactivatedAnimation, "png", unactivatedAnimationFile);
			ImageIO.write(activatedAnimation, "png", activatedAnimationFile);
		}

	}

	private void copyImagesToFolder(File newDir) throws IOException
	{
		File[] imageFilesToAnimate = getImageFilesToAnimate();
		for (File origin : imageFilesToAnimate)
		{
			String destinationFileNumberAndExtension = origin.getName().substring("temp".length());
			String destinationFileName = "frame" + destinationFileNumberAndExtension;
			File destination = new File(newDir, destinationFileName);
			Files.copy(origin.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

	}

}
