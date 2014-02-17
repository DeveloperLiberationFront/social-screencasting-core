package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.ncsu.lubick.localHub.LoadedFileEvent;
import edu.ncsu.lubick.localHub.LoadedFileListener;
import edu.ncsu.lubick.localHub.VideoFileListener;
import edu.ncsu.lubick.localHub.VideoFileMonitor;

public class BackgroundVideoUnpackingMonitor extends VideoFileMonitor {

	private Thread backgroundThread;

	private final BlockingQueue<File> unpackingQueue = new LinkedBlockingQueue<>();
	
	private boolean isRunning = true;

	private SingleCapFileExtractor capFileExtractor;

	public BackgroundVideoUnpackingMonitor(VideoFileListener localHub)
	{
		super(localHub);
		this.capFileExtractor = new SingleCapFileExtractor("./BackgroundVideo/", PostProductionHandler.FRAME_RATE);
		this.backgroundThread = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					while(isRunning)
					{
						File file = unpackingQueue.take();
						
						
						
					}
				}
				catch (Exception e)
				{
					logger.fatal("Shutting down because of Exception",e);
				}
				
			}
		});
		this.backgroundThread.setPriority(Thread.MIN_PRIORITY + 1);
	}

	
	@Override
	public int loadFileResponse(LoadedFileEvent e)
	{
		int response = super.loadFileResponse(e);
		if (response == LoadedFileListener.DONT_PARSE)
		{
			
		}
		return response;
	}
}
