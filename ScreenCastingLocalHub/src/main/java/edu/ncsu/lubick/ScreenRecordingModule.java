package edu.ncsu.lubick;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.wet.wired.jsr.recorder.DesktopScreenRecorder;
import com.wet.wired.jsr.recorder.ScreenRecorder;
import com.wet.wired.jsr.recorder.ScreenRecorderListener;

public class ScreenRecordingModule implements ScreenRecorderListener
{
	public static final String LOGGING_FILE_PATH = "/etc/log4j.settings";

	private static Logger logger;
	
	private File outputFolder;

	private ScreenRecorder recorder;

	private boolean isRecording;

	private boolean isPaused;

	//Static initializer to get the logging path set up and create the hub
	static {
		try
		{
			URL url = ScreenRecordingModule.class.getResource(LOGGING_FILE_PATH);
			PropertyConfigurator.configure(url);
			Logger.getRootLogger().info("Logging initialized");
		}
		catch (Exception e)
		{
			//load safe defaults
			BasicConfigurator.configure();
			Logger.getRootLogger().info("Could not load property file, loading defaults", e);
		}
		logger = Logger.getLogger(ScreenRecordingModule.class.getName());
	}

	public ScreenRecordingModule(File folderToOutput) {
		setOutputDirectory(folderToOutput);

		recorder = new DesktopScreenRecorder(folderToOutput, this);
	}




	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		File scratchDir = new File("scratch/");
		ScreenRecordingModule recordingModule = new ScreenRecordingModule(scratchDir);
		
		recordingModule.clearOutputDirectory();

		
		recordingModule.startRecording();
		
		logger.info("recording for 180 seconds" );
		System.out.println(new Date());
		for(int i = 1;i<=180;i++)
		{
			logger.info(i);
			Thread.sleep(1000);
		}


		recordingModule.stopRecording();
	}

	


	public void stopRecording() 
	{
		if (isRecording)
		{
			logger.debug("Screen recording module stopped");
			recorder.stopRecording();
			isRecording = false;
		}
	}




	public void startRecording() 
	{
		if (!isRecording)
		{
			logger.debug("Screen recording module started");
			recorder.startRecording();
			isRecording = true;
		}
	}
	
	public void pauseRecording() {
		if (!isPaused) {
			recorder.pause(true);
			isPaused = true;
		}
		
	}
	
	public void unpauseRecording() {
		if (isPaused) {
			recorder.pause(false);
			isPaused = false;
		}
		
	}




	private void clearOutputDirectory() {
		//clear out all the temp files
		for(File file: this.outputFolder.listFiles())
		{
			if (!file.isDirectory())
			{
				if (!file.delete())
				{
					logger.error("Could not clear out file "+file.getName()+ "in output folder");
				}
			}
		}
		
	}




	private void setOutputDirectory(File outputFolder) {
		
		if (!outputFolder.exists())
		{
			if (!outputFolder.mkdir())
			{
				throw new RuntimeException("Could not create scratch folder");
			}
		}
		this.outputFolder = outputFolder;
	}


	@Override
	public void frameRecorded(boolean fullFrame) throws IOException
	{
		//we don't care about this
	}

	@Override
	public void recordingStopped()
	{
		logger.info("Recording Stopped");

	}





}
