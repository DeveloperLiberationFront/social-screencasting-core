package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ImproperlyEncodedDateException;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.videoPostProduction.outputs.FramesToBrowserAnimatedPackage;
import edu.ncsu.lubick.util.FileUtilities;

public class PostProductionHandler
{
	public static final String MEDIA_OUTPUT_FOLDER = "renderedVideos\\";
	public static final String INTERMEDIATE_FILE_FORMAT = "jpg";
	public static final int FRAME_RATE = 5;
	public static final boolean DELETE_IMAGES_AFTER_USE = false;

	private static Logger logger = Logger.getLogger(PostProductionHandler.class.getName());

	private static final int RUN_UP_TIME = 5;

	private FramesToBrowserAnimatedPackage browserMediaMaker = null;
	
	private File screencastFolder;

	public PostProductionHandler(File sourceOfFrames, UserManager userManager)
	{
		this.screencastFolder = sourceOfFrames;
		this.browserMediaMaker = new FramesToBrowserAnimatedPackage(sourceOfFrames, userManager);

	}

	public File extractBrowserMediaForToolUsage(ToolUsage specificToolUse) throws MediaEncodingException
	{
		Date startTimeToLookFor = getStartingTime(specificToolUse);
		Date endTimeToLookFor = getEndTime(specificToolUse);

		File[] allFrames = getSortedFrameFiles();

		int startIndex = findFrameBelongingToDate(startTimeToLookFor, allFrames);
		if (startIndex >= allFrames.length)
		{
			logger.info("This tool use appears to be in the future, or at least later than "+allFrames[allFrames.length-1]);
			return null;
		}
		if (startIndex == 0)
		{
			if (checkForTooEarlyToolUsage(specificToolUse.getTimeStamp(),allFrames[0]))
			{
				return null;
			}
		}
		
		
		logger.debug("The first frame needed is at index " + startIndex + ", which corresponds to frame/file " + allFrames[startIndex]);

		int endIndex = findFrameBelongingToDate(endTimeToLookFor, allFrames);
		logger.debug("The last frame needed is at index " + endIndex + ", which corresponds to frame/file " + allFrames[endIndex]);
		
		browserMediaMaker.setSortedFrames(allFrames);
		File createdPackage = browserMediaMaker.combineImageFilesToMakeMedia(specificToolUse, startIndex, endIndex);

		return createdPackage;

	}

	private boolean checkForTooEarlyToolUsage(Date timeStamp, File firstFrame)
	{	//returns true if the timeStamp< firstFrame time
		
		Date parsedDate;
		try
		{
			parsedDate = FileUtilities.parseDateOfMediaFrame(firstFrame);
		}
		catch (ImproperlyEncodedDateException e)
		{
			logger.error("The frames are named different than convention dictate", e);
			return true;
		}
		
		return timeStamp.before(parsedDate);
	}

	private Date getEndTime(ToolUsage specificToolUse)
	{
		return new Date(specificToolUse.getTimeStamp().getTime() + specificToolUse.getDuration());
	}

	private Date getStartingTime(ToolUsage specificToolUse)
	{
		return new Date(specificToolUse.getTimeStamp().getTime() - 1000 * RUN_UP_TIME);
	}

	private File[] getSortedFrameFiles()
	{
		File[] allFrames = this.screencastFolder.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname)
			{
				return pathname.getName().endsWith(PostProductionHandler.INTERMEDIATE_FILE_FORMAT);
			}
		});
		Arrays.sort(allFrames);
		return allFrames;
	}

	private int findFrameBelongingToDate(Date date, File[] allFrames)
	{
		File goalFile = new File(this.screencastFolder, FileUtilities.encodeMediaFrameName(date));
		int startIndex = findIndexOfGoalFile(allFrames, goalFile);
		return startIndex;
	}

	private int findIndexOfGoalFile(File[] allFrames, File goalFile)
	{
		int startIndex = Arrays.binarySearch(allFrames, goalFile);

		startIndex = startIndex >= 0 ? startIndex : -1 - startIndex;
		return startIndex;
	}

}
