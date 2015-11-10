package com.wet.wired.jsr.recorder;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.imageio.ImageIO;

public class ThreadedDesktopRecorder extends DesktopScreenRecorder {

	private static final int THREAD_PRIORITY = Thread.NORM_PRIORITY-1;
	
	private ExecutorService workingThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
		
		@Override
		public Thread newThread(Runnable arg0)
		{
			Thread t = new Thread(arg0);
			t.setPriority(THREAD_PRIORITY);
			return t;
		}
	});
	
	
	public ThreadedDesktopRecorder(File outputFolder, ScreenRecorderListener listener)
	{
		super(outputFolder, listener);
	}
	
	@Override
	protected void writeImageToDisk(final RenderedImage bImage) throws IOException
	{
		
		final Date dateOfFrame = new Date();
		workingThreadPool.execute(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					ImageIO.write(bImage, "jpg", makeFile(dateOfFrame));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		
	}
	
	@Override
	public void stopRecording()
	{
		workingThreadPool.shutdown();
		super.stopRecording();
	}

}
