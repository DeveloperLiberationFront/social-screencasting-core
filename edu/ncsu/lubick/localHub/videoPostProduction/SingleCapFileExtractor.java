package edu.ncsu.lubick.localHub.videoPostProduction;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.util.BlockingImageDiskWritingStrategy;
import edu.ncsu.lubick.util.FileUtilities;
import edu.ncsu.lubick.util.ImageDiskWritingStrategy;

public class SingleCapFileExtractor {

	private ImageDiskWritingStrategy imageWriter;
	private Logger logger = Logger.getLogger(SingleCapFileExtractor.class.getName());
	private File currentCapFile;
	private FrameDecompressor decompressor = new FrameDecompressor();
	private Date capFileStartTime;
	private int frameRate;
	public static final String EXPECTED_SCREENCAST_FILE_EXTENSION = ".cap";

	public SingleCapFileExtractor(String outputPath, int frameRate)
	{
		this(new File(outputPath),frameRate);
	}

	public SingleCapFileExtractor(File outputDirectory, int frameRate)
	{
		this.imageWriter = new ChronologicalImageDiskWritingStrategy(outputDirectory);
		this.frameRate = frameRate;
	}

	public void loadFile(File capFile)
	{
		if (capFile == null)
		{
			logger.error("Recieved null file to load in CapFileExtractor");
			throw new IllegalArgumentException("A capFile cannot be null");
		}
		if (!capFile.getName().endsWith(EXPECTED_SCREENCAST_FILE_EXTENSION))
		{
			logger.error("Expected cap file to have an extension " + EXPECTED_SCREENCAST_FILE_EXTENSION + " not like " + capFile.getName());
			this.currentCapFile = null;
		}
		this.currentCapFile = capFile;

	}

	public void setStartTime(Date startTime)
	{
		this.capFileStartTime = startTime;
	}

	public void extractAllImages()
	{

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(this.currentCapFile.toPath())))
		{
			this.decompressor.readInFileHeader(inputStream);

			this.imageWriter.reset();

			extractAllImagesInStream(inputStream);

			this.imageWriter.waitUntilDoneWriting();
		}
		catch (IOException e)
		{
			logger.error("Problem extracting all the images", e);
		}

	}

	private void extractAllImagesInStream(InputStream inputStream) throws IOException
	{
		while (true)
		{
			BufferedImage tempImage = null;
			try
			{
				DecompressionFramePacket framePacket = decompressor.readInNextFrame(inputStream);
				tempImage = decompressor.createBufferedImageFromDecompressedFramePacket(framePacket);
			}
			catch (MediaEncodingException e)
			{
				logger.error("There was a problem making the video frames. Stopping extraction...", e);
				break;
			}
			catch (ReachedEndOfCapFileException e)
			{
				logger.info("reached the end of the cap file");
				break;
			}
			imageWriter.writeImageToDisk(tempImage);
		}
	}
	
	
	class ChronologicalImageDiskWritingStrategy extends BlockingImageDiskWritingStrategy{

		int currentFrame = 0;
		private Date currFrameDate;
		
		public ChronologicalImageDiskWritingStrategy(File outputDirectory)
		{
			super(outputDirectory, false);
			this.currFrameDate = new Date(capFileStartTime.getTime());
		}
		
		@Override
		protected String getNextFileName()
		{
			return FileUtilities.encodeMediaFrameName(currFrameDate);
		}
	}

}
