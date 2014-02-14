package edu.ncsu.lubick.localHub;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class VideoFileMonitor implements LoadedFileListener
{

	private VideoFileListener videoFileListener;
	private static Logger logger = Logger.getLogger(VideoFileMonitor.class.getName());


	public VideoFileMonitor(VideoFileListener localHub)
	{
		this.videoFileListener = localHub;
		
	}

	@Override
	public int loadFileResponse(LoadedFileEvent e)
	{
		if (e.getFileName().endsWith(PostProductionHandler.EXPECTED_SCREENCAST_FILE_EXTENSION))
		{
			if (!e.wasInitialReadIn())
			{
				logger.info("Found ScreenCapFile " + e.getFileName());
				this.videoFileListener.reportNewVideoFileLocation(e.getFullFileName());
			}
			return LoadedFileListener.DONT_PARSE;
		}

		return LoadedFileListener.NO_COMMENT;
	}

}