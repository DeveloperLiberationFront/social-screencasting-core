package edu.ncsu.lubick.localHub.videoPostProduction.gif;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class ThreadedAnimatedGifEncoder extends AnimatedGifEncoder 
{
	private static Logger logger = Logger.getLogger(ThreadedAnimatedGifEncoder.class.getName());
	private ExecutorService workingThreadPool;
	private BlockingQueue<BufferedImage> preconvertedImages = new ArrayBlockingQueue<>(2);
	
	private Object blockingObject = new Object();
	private boolean innerLoopContinue = true;

	public ThreadedAnimatedGifEncoder()
	{
		throw new RuntimeException("Not implemented yet");
//		this.workingThreadPool = Executors.newFixedThreadPool(1);
//		
//		Thread backThread = new Thread(new Runnable() {
//			
//			@Override
//			public void run()
//			{
//				while(innerLoopContinue)
//				{
//					try
//					{
//						BufferedImage nextImage = preconvertedImages.take();
//						ThreadedAnimatedGifEncoder.super.handleImageFrame(nextImage);
//					}
//					catch (InterruptedException e)
//					{
//						logger.fatal("Was interrupted making a GIF");
//					}
//					
//				}
//			}
//		});
//		backThread.setDaemon(true);
//		backThread.start();
	}
	
	@Override
	public void handleImageFrame(final BufferedImage frameImage)
	{
		if (!sizeSet)
		{
			// use first frame's size
			setSize(frameImage.getWidth(), frameImage.getHeight());
		}
		logger.trace("Starting converting Image to BGR");
		
		workingThreadPool.submit(new Runnable() {

			@Override
			public void run()
			{
				synchronized (blockingObject)
				{
					BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
					Graphics2D g = temp.createGraphics();
					g.drawImage(frameImage, 0, 0, null);
					try
					{
						preconvertedImages.put(temp);
					}
					catch (InterruptedException e)
					{
						logger.error("I was interrupted");
					}
				}
				logger.trace("converted");
			}
			
		});
	}
	
	private void waitUntilDoneWriting()
	{
		workingThreadPool.shutdown();
		
		try
		{
			workingThreadPool.awaitTermination(60, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			if (workingThreadPool.isTerminated())
			{
				logger.info("Thread pool was interrupted, but all the tasks finished", e);
			}
			else
			{
				logger.error("Thread pool was interrupted, and not all the tasks finished", e);
			}
		}
		innerLoopContinue = false;
	}
	
	@Override
	public boolean finish()
	{
		waitUntilDoneWriting();
		return super.finish();
	}
}
