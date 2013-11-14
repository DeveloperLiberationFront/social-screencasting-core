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

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedKeypressMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.AnimatedTextAndKeyboardMaker;
import edu.ncsu.lubick.localHub.videoPostProduction.animation.CornerKeypressAnimation;
import edu.ncsu.lubick.localHub.videoPostProduction.outputs.ImagesWithAnimationToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.outputs.PreAnimationImagesToMediaOutput;

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
	public static final String INTERMEDIATE_FILE_FORMAT = "png";
	public static final int FRAME_RATE = 5;
	public static final String EXPECTED_FILE_EXTENSION = ".cap";
	public static final boolean DELETE_IMAGES_AFTER_USE = false;

	private static final String SCRATCH_DIR = "./Scratch/";

	private static Logger logger = Logger.getLogger(PostProductionHandler.class.getName());

	private static final int RUN_UP_TIME = 5;

	private File currentCapFile;

	private Date capFileStartTime;

	private PostProductionAnimationStrategy postProductionAnimator;

	private Queue<OverloadFile> queueOfOverloadFiles = new LinkedList<>();
	private List<ImagesWithAnimationToMediaOutput> postAnimationMediaOutputs = new ArrayList<>();
	private List<PreAnimationImagesToMediaOutput> preAnimationMediaOutputs = new ArrayList<>();

	private FrameDecompressor decompressor = new FrameDecompressor();
	private ImageDiskWritingStrategy imageWriter;

	private double toolDemoInSeconds;
	private ToolUsage currentToolStream;

	public PostProductionHandler()
	{
		this.imageWriter = new ThreadedImageDiskWritingStrategy(SCRATCH_DIR, DELETE_IMAGES_AFTER_USE);

		AnimatedKeypressMaker animationSource = null;
		try
		{
			animationSource = new AnimatedTextAndKeyboardMaker();
		}
		catch (IOException e)
		{
			logger.info("Problem with the animations", e);
		}
		this.postProductionAnimator = new CornerKeypressAnimation(SCRATCH_DIR, FRAME_RATE, RUN_UP_TIME, animationSource);
	}

	public void loadFile(File capFile)
	{
		if (capFile == null)
		{
			logger.error("Recieved null file to load in PostProductionVideoHandler");
			throw new IllegalArgumentException("A capFile cannot be null");
		}
		if (!capFile.getName().endsWith(EXPECTED_FILE_EXTENSION))
		{
			logger.error("Expected cap file to have an extension " + EXPECTED_FILE_EXTENSION + " not like " + capFile.getName());
			this.currentCapFile = null;
		}
		this.currentCapFile = capFile;

	}

	public static void debugWriteOutAllImagesInCapFile(File capFile, File outputDirectory)
	{
		PostProductionHandler thisHandler = new PostProductionHandler();
		thisHandler.setCurrentFileStartTime(new Date());
		thisHandler.loadFile(capFile);
		if (!outputDirectory.mkdirs() && !outputDirectory.exists())
		{
			throw new RuntimeException("could not make output directory for debugWriteOutAllImagesInCapFile");
		}
		// set the output writer to be where we want
		thisHandler.imageWriter = new ThreadedImageDiskWritingStrategy(outputDirectory.getPath(), false);
		// write out all the images
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(thisHandler.currentCapFile.toPath())))
		{
			thisHandler.decompressor.readInFileHeader(inputStream);

			thisHandler.imageWriter.reset();

			thisHandler.extractAllImagesInStream(inputStream);

			thisHandler.imageWriter.waitUntilDoneWriting();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void setCurrentFileStartTime(Date startTime)
	{
		if (startTime == null)
		{
			logger.error("Recieved null startTime in PostProductionVideoHandler");
			throw new IllegalArgumentException("The start time cannot be null");
		}
		this.capFileStartTime = startTime;
		decompressor.setFrameZeroTime(capFileStartTime);
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

			String newFileNameStem = makeFileNameStemForToolPluginMedia(specificToolUse.getPluginName(), specificToolUse.getToolName());
			logger.info("Beginning the extraction of the frames");
			createdMediaFilesToReturn = extractDemoVideoToFile(inputStream, newFileNameStem);

		}
		catch (IOException e)
		{
			logger.error("There was a problem extracting the video", e);
			throw new MediaEncodingException("There was a problem extracting the video", e);
		}
		catch (ReachedEndOfCapFileException e)
		{
			logger.error("Unexpectedly hit the end of the cap file when seeking to start of tool usage");
			throw new MediaEncodingException(
					"Unexpectedly hit the end of the cap file when seeking to start of tool usage", e);
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

	private void fastFowardStreamToTime(InputStream inputStream, Date timeToLookFor) throws IOException, MediaEncodingException, ReachedEndOfCapFileException
	{
		if (timeToLookFor.equals(capFileStartTime)) // no fast forwarding required
		{
			return;
		}

		Date currTimeStamp = capFileStartTime;

		while (currTimeStamp.before(timeToLookFor))
		{
			decompressor.bypassNextFrame(inputStream); // throws VideoEncodingException if there was a problem
			currTimeStamp = decompressor.getPreviousFrameTimeStamp();

		}
	}

	private List<File> extractDemoVideoToFile(InputStream inputStream, String fileNameStem) throws IOException, MediaEncodingException, PostProductionAnimationException
	{
		imageWriter.reset();
		List<File> createdFiles = new ArrayList<>();

		extractImagesForTimePeriodToScratchFolder(inputStream);

		logger.debug("waiting until all the images are done extracting");
		imageWriter.waitUntilDoneWriting();

		createdFiles.addAll(handlePreAnimationMediaOutput(fileNameStem));

		if (postAnimationMediaOutputs.size() > 0)
		{
			addAnimationToImagesInScratchFolder();
			
			createdFiles.addAll(handleAnimationPostProduction(fileNameStem));
		}
		else
		{
			logger.info("Skipping animation step because no media outputs");
		}

		return createdFiles;
	}

	private List<File> handlePreAnimationMediaOutput(String fileNameStem) throws MediaEncodingException
	{
		List<File> createdFiles = new ArrayList<>();
		for (PreAnimationImagesToMediaOutput mediaOutput : preAnimationMediaOutputs)
		{
			createdFiles.add(mediaOutput.combineImageFilesToMakeMedia(fileNameStem, this.currentToolStream));
			logger.info(mediaOutput.getMediaTypeInfo() + " Rendered");
		}
		return createdFiles;
	}

	private void addAnimationToImagesInScratchFolder() throws PostProductionAnimationException
	{
		logger.info("Adding animation to video");
		try
		{
			postProductionAnimator.addAnimationToImagesInScratchFolderForToolStream(this.currentToolStream);
		}
		catch (IOException e)
		{
			throw new PostProductionAnimationException(e);
		}
	}

	public List<File> handleAnimationPostProduction(String fileName) throws MediaEncodingException
	{
		List<File> createdFiles = new ArrayList<>();

		logger.info("Rendering Media");

		for (ImagesWithAnimationToMediaOutput mediaOutput : postAnimationMediaOutputs)
		{
			createdFiles.add(mediaOutput.combineImageFilesToMakeMedia(fileName));		//throws MediaEncodingException if any problem
			logger.info(mediaOutput.getMediaTypeInfo() + " Rendered");
		}
		return createdFiles;
	}

	private void extractImagesForTimePeriodToScratchFolder(InputStream inputStream) throws IOException
	{
		logger.debug("starting extraction");
		for (int i = 0; i < FRAME_RATE * (RUN_UP_TIME + toolDemoInSeconds); i++)
		{
			BufferedImage tempImage = null;
			DecompressionFramePacket framePacket = null;
			try
			{
				framePacket = decompressor.readInNextFrame(inputStream);
				tempImage = decompressor.createBufferedImageFromDecompressedFramePacket(framePacket);

			}
			catch (MediaEncodingException e)
			{
				logger.error("There was a problem making the video frames.  Attempting to make a video from what I've got", e);
				break;
			}
			catch (ReachedEndOfCapFileException e)
			{
				if (queueOfOverloadFiles.size() == 0)
				{
					logger.error("Ran out of cap files to pull video from.  Truncated with what I've got");
					break;
				}
				inputStream = goToNextFileInQueue(inputStream);
				// call this image a mulligan and go to the top of the loop.
				i--;
				continue;
			}
			imageWriter.writeImageToDisk(tempImage);

		}
	}

	private void extractAllImagesInStream(InputStream inputStream) throws IOException
	{
		while (true)
		{
			BufferedImage tempImage = null;
			try
			{
				DecompressionFramePacket framePacket = decompressor.readInNextFrame(inputStream);
				tempImage = decompressor.createBufferedImageFromDecompressedFramePacket(framePacket);
			}
			catch (MediaEncodingException e)
			{
				logger.error("There was a problem making the video frames. Stopping extraction...", e);
				break;
			}
			catch (ReachedEndOfCapFileException e)
			{
				logger.info("reached the end of the cap file");
				break;
			}
			imageWriter.writeImageToDisk(tempImage);
		}
	}

	public static String makeFileNameStemForToolPluginMedia(String pluginName, String toolName)
	{
		if (toolName == null)
		{
			logger.info("Got a null toolname, recovering with empty string");
			toolName = "";
		}
		return "renderedVideos\\" + pluginName + createNumberForVideoFile(toolName);
	}

	private static int createNumberForVideoFile(String toolName)
	{
		int retval = toolName.hashCode();
		if (toolName.hashCode() == Integer.MIN_VALUE)
			retval = 0;
		return Math.abs(retval);
	}

	/**
	 * Adds an extra file to this handler to use if the wanted toolstream's behavior extends over the end of the loaded cap file
	 * 
	 * @param extraCapFile
	 * @param extraDate
	 */
	public void enqueueOverLoadFile(File extraCapFile, Date extraDate)
	{
		this.queueOfOverloadFiles.offer(new OverloadFile(extraCapFile, extraDate));

	}

	private InputStream goToNextFileInQueue(InputStream oldInputStream) throws IOException
	{
		oldInputStream.close();
		OverloadFile nextFile = queueOfOverloadFiles.poll();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(nextFile.file.toPath()));
		decompressor.readInFileHeader(inputStream);

		setCurrentFileStartTime(nextFile.date);

		return inputStream;
	}

	private static class OverloadFile
	{

		private Date date;
		private File file;

		public OverloadFile(File extraCapFile, Date extraDate)
		{
			this.file = extraCapFile;
			this.date = extraDate;
		}

	}

	public static String getIntermediateFolderLocation()
	{
		return SCRATCH_DIR;
	}

	public void addNewPostAnimationMediaOutput(ImagesWithAnimationToMediaOutput newMediaOutput)
	{
		this.postAnimationMediaOutputs.add(newMediaOutput);
	}

	public Set<ImagesWithAnimationToMediaOutput> getPostAnimationMediaOutputs()
	{
		return new HashSet<>(this.postAnimationMediaOutputs);
	}

	public void addNewPreAnimationMediaOutput(PreAnimationImagesToMediaOutput newMediaOutput)
	{
		this.preAnimationMediaOutputs.add(newMediaOutput);
	}

	public Set<PreAnimationImagesToMediaOutput> getPreAnimationMediaOutputs()
	{
		return new HashSet<>(this.preAnimationMediaOutputs);
	}

}
