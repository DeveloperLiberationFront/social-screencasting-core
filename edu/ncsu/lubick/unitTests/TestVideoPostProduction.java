package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

import edu.ncsu.lubick.localHub.FileUtilities;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.UtilitiesForTesting;
import edu.ncsu.lubick.localHub.videoPostProduction.ImagesToBrowserAnimatedPackage;
import edu.ncsu.lubick.localHub.videoPostProduction.ImagesToVideoOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.ThumbnailGenerator;
import edu.ncsu.lubick.localHub.videoPostProduction.VideoEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.gif.ImagesToGifOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.gif.ImagesToMiniGifOutput;

public class TestVideoPostProduction
{
	private static final String WHOMBO_TOOL_1 = "WhomboTool #1";
	private static final String TEST_PLUGIN_NAME = "Testing";
	private static final String DEFAULT_TESTING_KEYPRESS = "Ctrl+5";
	private static final String DEFAULT_TESTING_TOOL_CLASS = "Debug";

	private SimpleDateFormat dateInSecondsToNumber = FileUtilities.makeDateInSecondsToNumberFormatter();

	static
	{
		PropertyConfigurator.configure(LocalHub.LOGGING_FILE_PATH);
	}

	@Test
	public void testSingleToolUsageExtractionVideo() throws Exception
	{
		PostProductionHandler handler = makeVideoPostProductionHandler();

		List<File> outputMedia = testARandomToolInAPostAnimationHandler(handler);

		assertEquals(1, outputMedia.size());
		verifyVideoFileIsCorrectlyMade(outputMedia.get(0));
		verifyVideoNamedProperly(outputMedia.get(0), WHOMBO_TOOL_1);
	}

	@Test
	public void testSingleToolUsageExtractionGif() throws Exception
	{
		PostProductionHandler handler = makeGifPostProductionHandler();

		List<File> outputMedia = testARandomToolInAPostAnimationHandler(handler);

		assertEquals(1, outputMedia.size());
		verifyGifFileIsCorrectlyMade(outputMedia.get(0));
		verifyGifNamedProperly(outputMedia.get(0), WHOMBO_TOOL_1);
	}

	@Test
	public void testSingleToolUsageExtractionMiniGif() throws Exception
	{
		PostProductionHandler handler = makeMiniGifPostProductionHandler();

		List<File> outputMedia = testARandomToolInAPostAnimationHandler(handler);

		assertEquals(1, outputMedia.size());
		verifyGifFileIsCorrectlyMade(outputMedia.get(0));
		verifyGifNamedProperly(outputMedia.get(0), WHOMBO_TOOL_1);
	}
	
	@Test
	public void testSingleToolUsageExtractionBrowserMedia() throws Exception
	{
		String mediaDirName = PostProductionHandler.makeFileNameForToolPluginMedia(TEST_PLUGIN_NAME, WHOMBO_TOOL_1);
		File expectedOutputDir = new File(mediaDirName);
		if (expectedOutputDir.exists())
		{
			assertTrue(expectedOutputDir.isDirectory());
			assertTrue(UtilitiesForTesting.clearOutDirectory(expectedOutputDir));
			assertTrue(expectedOutputDir.delete());
			assertFalse(expectedOutputDir.exists());
		}
		
		
		PostProductionHandler handler = makeBrowserMediaPostProductionHandler();

		List<File> outputMedia = testARandomToolInAPostAnimationHandler(handler);

		assertEquals(1, outputMedia.size());
		assertEquals(expectedOutputDir, outputMedia.get(0));
		List<String> listOfFileNames = Arrays.asList(expectedOutputDir.list());
		assertTrue(listOfFileNames.size()>30);		//as of 11/7/13 this number was precisely 41, but this may change
															// if the rendering procedure changes.
		assertTrue(listOfFileNames.contains("image.png"));
		assertTrue(listOfFileNames.contains("image_un.png"));
		assertTrue(listOfFileNames.contains("image_text.png"));
		assertTrue(listOfFileNames.contains("image_text_un.png"));
		assertTrue(listOfFileNames.contains("text.png"));
		assertTrue(listOfFileNames.contains("text_un.png"));
		assertTrue(listOfFileNames.contains("frame0000.png"));
	}

	@Test
	public void testSingleToolUsageExtractionVideoThumbnailAndGif() throws Exception
	{
		PostProductionHandler handler = makeVideoThumbnailAndGifPostProductionHandler();

		List<File> outputMedia = testARandomToolInAPostAnimationHandler(handler);

		assertEquals(3, outputMedia.size());
		assertNotNull(outputMedia.get(0));
		verifyVideoFileIsCorrectlyMade(outputMedia.get(0));
		verifyVideoNamedProperly(outputMedia.get(0), WHOMBO_TOOL_1);

		assertNotNull(outputMedia.get(1));
		verifyGifFileIsCorrectlyMade(outputMedia.get(1));
		verifyGifNamedProperly(outputMedia.get(1), WHOMBO_TOOL_1);

		assertNotNull(outputMedia.get(2));
		verifyThumbnailFileIsCorrectlyMade(outputMedia.get(2));
		verifyThumbnailNamedProperly(outputMedia.get(2), WHOMBO_TOOL_1);
	}

	private List<File> testARandomToolInAPostAnimationHandler(PostProductionHandler handler) throws VideoEncodingException
	{
		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		String toolName = WHOMBO_TOOL_1;

		assertTrue(capFile.exists());

		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());

		handler.loadFile(capFile);

		handler.setCurrentFileStartTime(date);

		Date datePlusFifteen = new Date(date.getTime() + 15 * 1000); // plus
																		// fifteen
																		// seconds

		ToolUsage testToolUsage = makeToolUsage(datePlusFifteen, toolName);

		List<File> mediaOutputs = handler.extractMediaForToolUsage(testToolUsage);

		return mediaOutputs;
	}
	
	

	@Test
	public void testSingleToolUsageExtractionReallyEarly() throws Exception
	{

		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		String toolName = "WhomboTool #2";

		assertTrue(capFile.exists());

		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());

		PostProductionHandler handler = makeVideoPostProductionHandler();
		handler.loadFile(capFile);

		handler.setCurrentFileStartTime(date);

		Date datePlusOne = new Date(date.getTime() + 1 * 1000); // plus one
																// second

		ToolUsage testToolUsage = makeToolUsage(datePlusOne, toolName);

		File outputFile = getVideoFromHandler(handler, testToolUsage);

		verifyVideoFileIsCorrectlyMade(outputFile);
		verifyVideoNamedProperly(outputFile, toolName);
	}

	@Test
	public void testSingleToolUsageExtractionOverlappingFiles() throws Exception
	{

		File firstcapFile = new File("./src/ForTesting/oneMinuteCap.cap");
		File secondCapFile = new File("./src/ForTesting/oneMinuteCap.cap"); // we'll just reuse this for testing
		String toolName = "WhomboTool #3";

		assertTrue(firstcapFile.exists());
		assertTrue(secondCapFile.exists());

		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());
		Date secondDate = UtilitiesForTesting.truncateTimeToMinute(new Date(date.getTime() + 61 * 1000));

		PostProductionHandler handler = makeVideoPostProductionHandler();
		handler.loadFile(firstcapFile);
		handler.enqueueOverLoadFile(secondCapFile, secondDate);

		handler.setCurrentFileStartTime(date);

		Date datePlusFiftyFive = new Date(date.getTime() + 55 * 1000); // plus 55 seconds, plenty to over run this file

		ToolUsage testToolUsage = makeToolUsage(datePlusFiftyFive, toolName, 10 * 1000);

		File outputFile = getVideoFromHandler(handler, testToolUsage);

		verifyVideoFileIsCorrectlyMade(outputFile);
		verifyVideoNamedProperly(outputFile, toolName);

	}

	@Test
	public void testBug8() throws Exception
	{ // https://bitbucket.org/klubick/screencasting-module/issue/8/extracting-some-tools-causes-a
		// This was an overlap problem.

		File firstcapFile = new File("./src/ForTesting/bug8.screencasts.28913211516.cap");
		File secondCapFile = new File("./src/ForTesting/bug8.screencasts.28913211615.cap");

		assertTrue(firstcapFile.exists());
		assertTrue(secondCapFile.exists());

		Date date = dateInSecondsToNumber.parse("28913211516");
		Date secondDate = dateInSecondsToNumber.parse("28913211615");

		assertNotNull(date);
		assertNotNull(secondDate);

		PostProductionHandler handler = makeVideoPostProductionHandler();
		handler.loadFile(firstcapFile);
		handler.enqueueOverLoadFile(secondCapFile, secondDate);

		handler.setCurrentFileStartTime(date);

		Date dateOfBuggyToolUsage = new Date(1381972574596L); // from the bug
																// report.

		IdealizedToolStream iToolStream = new IdealizedToolStream(date);

		iToolStream.addToolUsage("Open Call Hierarchy", "", "MENU", dateOfBuggyToolUsage, 15000);

		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin("Eclipse");
		assertEquals(1, toolStream.getAsList().size());

		File outputFile = null;
		try
		{
			outputFile = getVideoFromHandler(handler, toolStream.getAsList().get(0));
			// throws an exception if there was a problem
		}
		catch (VideoEncodingException e)
		{
			e.printStackTrace();
			fail("Should not have caught an exception here");
		}

		verifyVideoFileIsCorrectlyMade(outputFile);

	}

	private ToolUsage makeToolUsage(Date toolUsageDate, String toolUsageName)
	{
		return makeToolUsage(toolUsageDate, toolUsageName, 2000);
	}

	private ToolUsage makeToolUsage(Date toolUsageDate, String toolUsageName, int duration)
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(toolUsageDate);
		iToolStream.addToolUsage(toolUsageName, DEFAULT_TESTING_TOOL_CLASS, DEFAULT_TESTING_KEYPRESS, toolUsageDate, duration);

		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin(TEST_PLUGIN_NAME);
		assertEquals(1, toolStream.getAsList().size());

		ToolUsage testToolUsage = toolStream.getAsList().get(0);
		return testToolUsage;
	}

	private void verifyVideoFileIsCorrectlyMade(File outputFile)
	{
		assertNotNull(outputFile);
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		assertTrue(outputFile.getName().endsWith(ImagesToVideoOutput.VIDEO_EXTENSION));
		assertTrue(outputFile.length() > 500000); // I expect the file size to
													// be at least 1 Mb and no
													// more than 2Mb
		assertTrue(outputFile.length() < 2000000);
	}

	private void verifyGifNamedProperly(File outputFile, String toolName)
	{
		assertTrue(outputFile.getPath().startsWith(PostProductionHandler.makeFileNameForToolPluginMedia(TEST_PLUGIN_NAME, toolName)));
		assertTrue(outputFile.getPath().endsWith(".gif"));
	}

	private void verifyGifFileIsCorrectlyMade(File outputFile)
	{
		assertNotNull(outputFile);
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		assertTrue(outputFile.length() > 500000);
		assertTrue(outputFile.length() < 10 * 1000 * 1000);
	}

	private void verifyThumbnailNamedProperly(File outputFile, String toolName)
	{
		assertTrue(outputFile.getPath().startsWith(PostProductionHandler.makeFileNameForToolPluginMedia(TEST_PLUGIN_NAME, toolName)));
		assertTrue(outputFile.getPath().endsWith(ThumbnailGenerator.THUMBNAIL_EXTENSION));
	}

	private void verifyThumbnailFileIsCorrectlyMade(File outputFile)
	{
		assertNotNull(outputFile);
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		assertTrue(outputFile.length() > 500);
		assertTrue(outputFile.length() < 2 * 1000 * 1000);
	}

	private void verifyVideoNamedProperly(File outputFile, String toolName)
	{
		assertTrue(outputFile.getPath().startsWith(PostProductionHandler.makeFileNameForToolPluginMedia(TEST_PLUGIN_NAME, toolName)));
		assertTrue(outputFile.getPath().endsWith(ImagesToVideoOutput.VIDEO_EXTENSION));
	}

	public static File getVideoFromHandler(PostProductionHandler handler, ToolUsage testToolUsage) throws VideoEncodingException
	{
		List<File> mediaOutputs = handler.extractMediaForToolUsage(testToolUsage);
		if (mediaOutputs == null)
		{
			return null;
		}
		for (File f : mediaOutputs)
		{
			if (f.getName().endsWith(ImagesToVideoOutput.VIDEO_EXTENSION))
				return f;
		}
		return null;
	}

	private PostProductionHandler makeVideoPostProductionHandler()
	{
		PostProductionHandler handler = new PostProductionHandler();
		handler.addNewPostAnimationMediaOutput(new ImagesToVideoOutput());
		return handler;
	}

	private PostProductionHandler makeGifPostProductionHandler()
	{
		PostProductionHandler handler = new PostProductionHandler();
		handler.addNewPostAnimationMediaOutput(new ImagesToGifOutput());
		return handler;
	}

	private PostProductionHandler makeMiniGifPostProductionHandler()
	{
		PostProductionHandler handler = new PostProductionHandler();
		handler.addNewPostAnimationMediaOutput(new ImagesToMiniGifOutput());
		return handler;
	}

	private PostProductionHandler makeVideoThumbnailAndGifPostProductionHandler()
	{
		PostProductionHandler handler = new PostProductionHandler();
		handler.addNewPostAnimationMediaOutput(new ImagesToVideoOutput());
		handler.addNewPostAnimationMediaOutput(new ImagesToGifOutput());
		handler.addNewPostAnimationMediaOutput(new ThumbnailGenerator());
		return handler;
	}
	
	private PostProductionHandler makeBrowserMediaPostProductionHandler()
	{
		PostProductionHandler handler = new PostProductionHandler();
		handler.addNewPreAnimationMediaOutput(new ImagesToBrowserAnimatedPackage());
		return handler;
	}

}
