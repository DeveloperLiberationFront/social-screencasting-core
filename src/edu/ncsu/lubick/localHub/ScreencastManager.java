package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.util.FileUtilities;

/**
 * Deletes images in the screencast folder after they are a certain amount of time old
 * @author Kevin Lubick
 *
 */
public class ScreencastManager extends TimerTask{

	public static final int LIFE_OF_SCREENCAST = 300_000;		//300 seconds

	protected File folderToMonitor;

	private static Timer t = null;

	private static final Logger logger = Logger.getLogger(ScreencastManager.class);

	private ScreencastManager(File folderToMonitor){

		this.folderToMonitor = folderToMonitor;

	}

	public static void startManaging(File folderToMonitor)
	{
		synchronized (logger)		//synchronized because FindBugs said so
		{
			if (t!=null)
			{
				return;
			}
			
			logger.info("Scheduling new screencast monitor");
			
			t = new Timer(true);
			ScreencastManager sm = new ScreencastManager(folderToMonitor);


			t.schedule(sm, LIFE_OF_SCREENCAST, LIFE_OF_SCREENCAST);
		}
		
	}

	public static void stopManaging()
	{
		t.cancel();
		t = null;
	}

	@Override
	public void run()
	{
		final Date cutoffDate = new Date();
		cutoffDate.setTime(cutoffDate.getTime()-LIFE_OF_SCREENCAST);
		logger.info("Deleting all files older than "+cutoffDate);

		final String[] frameNames = folderToMonitor.list();	//listing the strings is more efficient than
															//allocating the files up front.  See source for File.java

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run()
			{
				for(String frameName: frameNames)
				{
					try
					{
						if (FileUtilities.parseDateOfMediaFrame(frameName).before(cutoffDate))
						{
							File fileToDelete = new File(folderToMonitor, frameName);
							logger.trace("Frame "+fileToDelete+" was deleted: "+fileToDelete.delete());
						}
						//TODO perhaps break on the else
					}
					catch (ImproperlyEncodedDateException e)
					{
						logger.error("Problem parsing Screencasting frame",e);
					}
				}
			}
		});

		thread.setPriority(Thread.MIN_PRIORITY+1);
		thread.start();
	}
}
