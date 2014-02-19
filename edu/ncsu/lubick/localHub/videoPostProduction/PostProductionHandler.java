package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ImproperlyEncodedDateException;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedTextAndKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.CornerKeypressAnimation;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.KeypressAnimationMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.outputs.ImagesWithAnimationToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.outputs.PreAnimationImagesToBrowserAnimatedPackage;
import edu.ncsu.lubick.localHub.videoPostProduction.outputs.PreAnimationImagesToMediaOutput;
import edu.ncsu.lubick.util.FileDateStructs;
import edu.ncsu.lubick.util.FileUtilities;
import edu.ncsu.lubick.util.ThreadedImageDiskWritingStrategy;

public class PostProductionHandler
{
	public static final String MEDIA_OUTPUT_FOLDER = "renderedVideos\\";
	public static final String INTERMEDIATE_FILE_FORMAT = "jpg";
	public static final int FRAME_RATE = 5;
	public static final boolean DELETE_IMAGES_AFTER_USE = false;

	private static final String MEDIA_ASSEMBLY_DIR = "./MediaAssembly/";


	public static Logger logger = Logger.getLogger(PostProductionHandler.class.getName());

	private static final int RUN_UP_TIME = 5;

	PreAnimationImagesToBrowserAnimatedPackage browserMediaMaker = new PreAnimationImagesToBrowserAnimatedPackage();

	private double toolDemoInSeconds;
	private ToolUsage currentToolStream;
	private ThreadedImageDiskWritingStrategy imageWriter;
	private CornerKeypressAnimation postProductionAnimator;
	private File screencastFolder;

	public PostProductionHandler(File sourceOfFrames)
	{
		this.screencastFolder = sourceOfFrames;
		
		
		/*this.imageWriter = new ThreadedImageDiskWritingStrategy(MEDIA_ASSEMBLY_DIR, DELETE_IMAGES_AFTER_USE);

		KeypressAnimationMaker animationSource = null;
		try
		{
			animationSource = new AnimatedTextAndKeyboardMaker();
		}
		catch (IOException e)
		{
			logger.info("Problem with the animations", e);
		}
		this.postProductionAnimator = new CornerKeypressAnimation(MEDIA_ASSEMBLY_DIR, FRAME_RATE, RUN_UP_TIME, animationSource);
		*/
	}

	public File extractBrowserMediaForToolUsage(ToolUsage specificToolUse) throws MediaEncodingException
	{
		Date timeToLookFor = specificToolUse.getTimeStamp();
		
		File[] allFrames = this.screencastFolder.listFiles();
		Arrays.sort(allFrames);
		
		int startIndex = Arrays.binarySearch(allFrames, FileUtilities.encodeMediaFrameName(timeToLookFor));
		
		logger.info(allFrames[startIndex]);
		return null;
		
		
		/*Date timeToLookFor = specificToolUse.getTimeStamp();

		logger.info("The video for " + specificToolUse.toString() + " will start at " + timeToLookFor);

		
		
		
		this.toolDemoInSeconds = specificToolUse.getDuration() / 1000.0;

		this.currentToolStream = specificToolUse;

		List<File> createdMediaFilesToReturn = null;
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(currentCapFile.toPath())))
		{
			decompressor.readInFileHeader(inputStream);
			logger.info("Fast forwarding to the appropriate time");
			
			String newFileNameStem = FileUtilities.makeFileNameStemForToolPluginMedia(specificToolUse);
			logger.info("Beginning the extraction of the frames");
			createdMediaFilesToReturn = extractDemoVideoToFile(inputStream, newFileNameStem);

		}
		catch (IOException e)
		{
			logger.error("There was a problem extracting the video", e);
			throw new MediaEncodingException("There was a problem extracting the video", e);
		}
		catch (PostProductionAnimationException e)
		{
			throw new MediaEncodingException("problem with the animation production", e);
		}

		return createdMediaFilesToReturn;*/
	}


//	private List<File> extractDemoVideoToFile(InputStream inputStream, String fileNameStem) throws IOException, MediaEncodingException,
//	PostProductionAnimationException
//	{
//		imageWriter.reset();
//		List<File> createdFiles = new ArrayList<>();
//
//		extractImagesForTimePeriodToScratchFolder(inputStream);
//
//		logger.debug("waiting until all the images are done extracting");
//		imageWriter.waitUntilDoneWriting();
//
//		createdFiles.addAll(handlePreAnimationMediaOutput(fileNameStem));
//
//
//		return createdFiles;
//	}

	private List<File> handlePreAnimationMediaOutput(String fileNameStem) throws MediaEncodingException
	{
		List<File> createdFiles = new ArrayList<>();

		createdFiles.add(browserMediaMaker.combineImageFilesToMakeMedia(fileNameStem, this.currentToolStream));
		logger.info(browserMediaMaker.getMediaTypeInfo() + " Rendered");
		return createdFiles;
	}


	public static String getIntermediateFolderLocation()
	{
		return MEDIA_ASSEMBLY_DIR;
	}


	public void reset()
	{
		

	}



}
