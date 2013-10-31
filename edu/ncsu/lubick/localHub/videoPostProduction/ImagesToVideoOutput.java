package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class ImagesToVideoOutput implements ImagesToMediaOutput 
{
	public static final String VIDEO_EXTENSION = "mkv";
	private static Logger logger = Logger.getLogger(ImagesToVideoOutput.class.getName());
	
	@Override
	public File combineImageFilesToMakeMedia(String fileNameMinusExtension) throws IOException
	{
		File newVideoFile = makeVideoFile(fileNameMinusExtension);
		
		if (!newVideoFile.getParentFile().mkdirs() && !newVideoFile.getParentFile().exists())
		{
			throw new IOException("Could not make the output folder " + newVideoFile.getParentFile());
		}
		// TODO make this more flexible, not hardcoded. i.e. the user should
		// specify where their ffmpeg is

		String executableString = compileExecutableString(newVideoFile);

		// Using Runtime.exec() because I couldn't get ProcessBuilder to handle the arguments on ffempeg well.
		Process process = Runtime.getRuntime().exec(executableString);

		inheritIO(process.getInputStream(), "Normal Output");
		inheritIO(process.getErrorStream(), "Error Output");
		logger.debug("Rendering video");
		try
		{
			logger.debug("FFMPEG exited with state " + process.waitFor());
		}
		catch (InterruptedException e)
		{
			logger.error("There was a problem with ffmpeg", e);
		}
		
		return newVideoFile;
	}
	
	private File makeVideoFile(String fileNameMinusExtension)
	{
		return new File(fileNameMinusExtension + "." + VIDEO_EXTENSION);
	}

	private String compileExecutableString(File newVideoFile)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("./src/FFMPEGbin/ffmpeg.exe -r 5 -pix_fmt yuv420p -i ");
		builder.append(PostProductionHandler.getIntermediateFolderLocation());
		builder.append("temp%04d.");
		builder.append(PostProductionHandler.INTERMEDIATE_FILE_FORMAT);
		builder.append("  -vcodec libx264 ");
		builder.append(newVideoFile.getPath());
		return builder.toString();
	}

	private static void inheritIO(final InputStream src, final String identifer)
	{
		// used to spy on a process's output, similar to what ProcessBuilder
		// does
		new Thread(new Runnable() {
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

}
