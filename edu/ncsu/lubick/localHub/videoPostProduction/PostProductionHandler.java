package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
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

/* Some parts of this (the decoding aspect) have the following license:
 * 
 * This software is OSI Certified Open Source Software
 * 
 * The MIT License (MIT)
 * Copyright 2000-2001 by Wet-Wired.com Ltd., Portsmouth England
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 */
public class PostProductionHandler
{
	public static final String MEDIA_OUTPUT_FOLDER = "renderedVideos\\";
	public static final String INTERMEDIATE_FILE_FORMAT = "jpg";
	public static final int FRAME_RATE = 5;
	public static final boolean DELETE_IMAGES_AFTER_USE = false;

	private static final String MEDIA_ASSEMBLY_DIR = "./MediaAssembly/";

	public static Logger logger = Logger.getLogger(PostProductionHandler.class.getName());

	private static final int RUN_UP_TIME = 5;
	private Date capFileStartTime;

	private PostProductionAnimationStrategy postProductionAnimator;

	private Queue<FileDateStructs> queueOfOverloadFiles = new LinkedList<>();
	private Set<PreAnimationImagesToMediaOutput> preAnimationMediaOutputs = new HashSet<>();

	PreAnimationImagesToBrowserAnimatedPackage browserMediaMaker = new PreAnimationImagesToBrowserAnimatedPackage();

	private double toolDemoInSeconds;
	private ToolUsage currentToolStream;
	private ThreadedImageDiskWritingStrategy imageWriter;

	public PostProductionHandler()
	{
		this.imageWriter = new ThreadedImageDiskWritingStrategy(MEDIA_ASSEMBLY_DIR, DELETE_IMAGES_AFTER_USE);

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
	}

	public List<File> extractMediaForToolUsage(ToolUsage specificToolUse) throws MediaEncodingException
	{
		if (this.currentCapFile == null || this.capFileStartTime == null)
		{
			logger.error("PostProductionVideo object needed to have a file to load and a start time");
			throw new MediaEncodingException("PostProductionVideo object needed to have a file to load and a start time");
		}
		if (this.postAnimationMediaOutputs.size() == 0 && this.preAnimationMediaOutputs.size() == 0)
		{
			logger.info("No media outputs, so nothing was done");
			return new ArrayList<>();
		}

		Date timeToLookFor = findStartingTime(specificToolUse);

		logger.info("The video for " + specificToolUse.toString() + " will start at " + timeToLookFor);

		this.toolDemoInSeconds = specificToolUse.getDuration() / 1000.0;

		this.currentToolStream = specificToolUse;

		List<File> createdMediaFilesToReturn = null;
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(currentCapFile.toPath())))
		{
			decompressor.readInFileHeader(inputStream);
			logger.info("Fast forwarding to the appropriate time");
			fastFowardStreamToTime(inputStream, timeToLookFor); // throws VideoEncodingException if there was a problem prior to the important bits

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

		return createdMediaFilesToReturn;
	}

	private Date findStartingTime(ToolUsage specificToolUse)
	{
		Date timeToLookFor = new Date(specificToolUse.getTimeStamp().getTime() - RUN_UP_TIME * 1000);
		if (timeToLookFor.before(capFileStartTime))
		{
			timeToLookFor = capFileStartTime;
		}
		return timeToLookFor;
	}


	private List<File> extractDemoVideoToFile(InputStream inputStream, String fileNameStem) throws IOException, MediaEncodingException,
	PostProductionAnimationException
	{
		imageWriter.reset();
		List<File> createdFiles = new ArrayList<>();

		extractImagesForTimePeriodToScratchFolder(inputStream);

		logger.debug("waiting until all the images are done extracting");
		imageWriter.waitUntilDoneWriting();

		createdFiles.addAll(handlePreAnimationMediaOutput(fileNameStem));


		return createdFiles;
	}

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
