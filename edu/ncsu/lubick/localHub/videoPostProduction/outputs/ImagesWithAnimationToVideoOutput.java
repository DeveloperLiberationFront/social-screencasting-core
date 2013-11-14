package edu.ncsu.lubick.localHub.videoPostProduction.outputs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.videoPostProduction.AbstractImagesToMediaOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class ImagesWithAnimationToVideoOutput extends AbstractImagesToMediaOutput implements ImagesWithAnimationToMediaOutput
{
	public ImagesWithAnimationToVideoOutput()
	{
		super(new File(PostProductionHandler.getIntermediateFolderLocation()));
	}

	public static final String VIDEO_EXTENSION = "mkv";
	private static Logger logger = Logger.getLogger(ImagesWithAnimationToVideoOutput.class.getName());

	@Override
	public File combineImageFilesToMakeMedia(String fileNameMinusExtension) throws MediaEncodingException
	{
		try
		{
			File newVideoFile = makeVideoFile(fileNameMinusExtension);

			String executableString = compileExecutableString(newVideoFile);

			// Using Runtime.exec() because I couldn't get ProcessBuilder to handle the arguments on ffempeg well.
			Process process = Runtime.getRuntime().exec(executableString);

			setUpLoggingForProcess(process);

			return newVideoFile;
		}
		catch (IOException e)
		{
			throw new MediaEncodingException(e);
		}
	}

	protected void setUpLoggingForProcess(Process process)
	{
		inheritIO(process.getInputStream(), "Normal Output");
		inheritIO(process.getErrorStream(), "Error Output");
		logger.info("Rendering video");
		try
		{
			int processReturnValue = process.waitFor();
			logger.debug("FFMPEG exited with state " + processReturnValue);
			if (processReturnValue != 0)
			{
				logger.info("Non zero return value from FFMPEG: "+processReturnValue);
			}
		}
		catch (InterruptedException e)
		{
			logger.error("There was a problem with ffmpeg", e);
		}
	}

	private File makeVideoFile(String fileNameMinusExtension) throws MediaEncodingException
	{
		File newFile = new File(fileNameMinusExtension + "." + VIDEO_EXTENSION);
		cleanUpForFile(newFile);

		return newFile;
	}

	private String compileExecutableString(File newVideoFile)
	{
		// TODO make this more flexible, not hardcoded. i.e. the user should
		// specify where their ffmpeg is
		StringBuilder builder = new StringBuilder();
		builder.append("./src/FFMPEGbin/ffmpeg.exe -r 5 -pix_fmt yuv420p -i ");
		builder.append(scratchDir);
		builder.append("\\temp%04d.");
		builder.append(PostProductionHandler.INTERMEDIATE_FILE_FORMAT);
		builder.append("  -vcodec libx264 ");
		builder.append(newVideoFile.getPath());
		return builder.toString();
	}

	private static void inheritIO(final InputStream src, final String identifer)
	{
		// used to spy on a process's output, similar to what ProcessBuilder does
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				try (Scanner sc = new Scanner(src);)
				{
					while (sc.hasNextLine())
					{
						String string = sc.nextLine();
						logger.trace("From " + identifer + ":" + string);
					}
				}
				catch (Exception e)
				{
					logger.error("Problem in stream monitoring", e);
				}

			}
		}).start();
	}

	@Override
	public String getMediaTypeInfo()
	{
		return "Video/mkv";
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
