package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ImproperlyEncodedDateException;
import edu.ncsu.lubick.localHub.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.videoPostProduction.outputs.FramesToBrowserAnimatedPackage;
import edu.ncsu.lubick.util.FileUtilities;

public class PostProductionHandler
{
	public static final String MEDIA_OUTPUT_FOLDER = "renderedVideos"+File.separator;
	public static final String FULLSCREEN_IMAGE_FORMAT = "jpg";
	public static final String ANIMATION_FORMAT = "png";
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
		if (allFrames.length == 0)
		{
			logger.error("Could not extract tool usage because the screencasting folder is empty");
			return null;
		}
		if (startIndex >= allFrames.length)
		{
			logger.info("This tool use appears to be in the future, or at least later than "+allFrames[allFrames.length-1]);
			return null;
		}
		if (startIndex == 0)
		{
			if (checkForTooEarlyToolUsage(specificToolUse.getTimeStamp(),allFrames[0]))
			{
				logger.info("Tool usage was not extracted because it happens before the screencasting was recorded");
				return null;
			}
		}
		
		
		logger.debug("The first frame needed is at index " + startIndex + ", which corresponds to frame/file " + allFrames[startIndex]);

		int endIndex = findFrameBelongingToDate(endTimeToLookFor, allFrames);
		logger.debug("The last frame needed is at index " + endIndex + ", which corresponds to frame/file " + allFrames[endIndex]);
		
		browserMediaMaker.setSortedFrames(allFrames);
		return browserMediaMaker.combineImageFilesToMakeMedia(specificToolUse, startIndex, endIndex);
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
				return pathname.getName().endsWith(PostProductionHandler.FULLSCREEN_IMAGE_FORMAT);
			}
		});
		Arrays.sort(allFrames);
		return allFrames;
	}

	private int findFrameBelongingToDate(Date date, File[] allFrames)
	{
		File goalFile = new File(this.screencastFolder, FileUtilities.encodeMediaFrameName(date));
		return findIndexOfGoalFile(allFrames, goalFile);
	}

	private int findIndexOfGoalFile(File[] allFrames, File goalFile)
	{
		int startIndex = Arrays.binarySearch(allFrames, goalFile);

		return startIndex >= 0 ? startIndex : -1 - startIndex;
	}

}
