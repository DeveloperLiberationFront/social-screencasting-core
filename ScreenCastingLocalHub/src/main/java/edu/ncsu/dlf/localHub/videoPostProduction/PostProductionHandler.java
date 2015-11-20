package edu.ncsu.dlf.localHub.videoPostProduction;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ncsu.dlf.localHub.ImproperlyEncodedDateException;
import edu.ncsu.dlf.localHub.ToolUsage;
import edu.ncsu.dlf.localHub.UserManager;
import edu.ncsu.dlf.localHub.videoPostProduction.outputs.FramesToBrowserAnimatedPackage;
import edu.ncsu.dlf.util.FileUtilities;

public class PostProductionHandler
{
	public static final String MEDIA_OUTPUT_FOLDER = "renderedVideos"+File.separator;
	public static final String FULLSCREEN_IMAGE_FORMAT = "jpg";
	public static final String ANIMATION_FORMAT = "png";
	public static final int FRAME_RATE = 5;
	public static final boolean DELETE_IMAGES_AFTER_USE = false;

	private static Logger logger = Logger.getLogger(PostProductionHandler.class.getName());

	private static final int RUN_UP_TIME = 5000;
	private static final int MINIMUM_VID_TIME = 2000;

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
			throw new MediaEncodingException("Could not extract tool usage because the screencasting folder is empty");
		}
		if (startIndex >= allFrames.length)
		{
			throw new MediaEncodingException("This tool use appears to be in the future ("+startTimeToLookFor +"), or at least later than "+allFrames[allFrames.length-1]);
		}
		if (startIndex <= 0)
		{
			if (checkForTooEarlyToolUsage(specificToolUse.getTimeStamp(),allFrames[0]))
			{
				throw new MediaEncodingException("Tool usage was not extracted because it happens before the screencasting was recorded");
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
		return new Date(specificToolUse.getTimeStamp().getTime() + Math.min(MINIMUM_VID_TIME, specificToolUse.getDuration()));
	}

	private Date getStartingTime(ToolUsage specificToolUse)
	{
		return new Date(specificToolUse.getTimeStamp().getTime() - RUN_UP_TIME);
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
		if (allFrames != null) {
			Arrays.sort(allFrames);
		} else {
			allFrames = new File[0];
		}
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
