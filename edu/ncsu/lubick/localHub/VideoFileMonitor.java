package edu.ncsu.lubick.localHub;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.SingleCapFileExtractor;

public class VideoFileMonitor implements LoadedFileListener
{

	private VideoFileListener videoFileListener;
	protected Logger logger; 


	public VideoFileMonitor(VideoFileListener localHub)
	{
		logger = Logger.getLogger(getClass().getName());
		this.videoFileListener = localHub;
		
	}

	@Override
	public int loadFileResponse(LoadedFileEvent e)
	{
		if (e.getFileName().endsWith(SingleCapFileExtractor.EXPECTED_SCREENCAST_FILE_EXTENSION))
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