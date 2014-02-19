package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.Runner;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.UtilitiesForTesting;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.outputs.PreAnimationImagesToBrowserAnimatedPackage;
import edu.ncsu.lubick.util.FileDateStructs;
import edu.ncsu.lubick.util.FileUtilities;

public class TestVideoPostProduction
{
	
	//private static final Logger logger = Logger.getLogger(TestVideoPostProduction.class);
	
	private static final String WHOMBO_TOOL_1 = "WhomboTool #1";
	private static final String TEST_PLUGIN_NAME = "Testing";
	private static final String DEFAULT_TESTING_KEYPRESS = "Ctrl+5";
	private static final String DEFAULT_TESTING_TOOL_CLASS = "Debug";

	@BeforeClass
	public static void setUpBeforeAll()
	{
		RemoteSQLDatabaseFactory.setUpToUseMockDB(true);
		try
		{
			URL url = Runner.class.getResource(LocalHub.LOGGING_FILE_PATH);
			PropertyConfigurator.configure(url);
			Logger.getRootLogger().info("Logging initialized");
		}
		catch (Exception e)
		{
			//load safe defaults
			BasicConfigurator.configure();
			Logger.getRootLogger().info("Could not load property file, loading defaults", e);
		}
		RemoteSQLDatabaseFactory.setUpToUseMockDB(true);
	}


	@Test
	public void testFullCapFileExtraction() throws Exception
	{
		File outputDirectory = new File("./test/");
		UtilitiesForTesting.clearOutDirectory(outputDirectory);
		
		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		
		
		PostProductionHandler.debugWriteOutAllImagesInCapFile(capFile, outputDirectory);
	}


	@Test
	public void testSingleToolUsageExtractionBrowserMedia() throws Exception
	{
		Date truncatedDate = UtilitiesForTesting.truncateTimeToMinute(new Date());
		ToolUsage sampleToolUsage = makeToolUsage(truncatedDate, WHOMBO_TOOL_1);
		
		File expectedOutputDir = prepareForBrowserMediaTest(sampleToolUsage);

		PostProductionHandler handler = makeBrowserMediaPostProductionHandler();

		List<File> outputMedia = testARandomToolInAPostAnimationHandler(handler);

		verifyBrowserMediaCreatedCorrectly(expectedOutputDir, outputMedia);
	}


	private File prepareForBrowserMediaTest(ToolUsage testToolUsage)
	{
		String mediaDirName = PostProductionHandler.makeFileNameStemForToolPluginMedia(testToolUsage);
		File expectedOutputDir = new File(mediaDirName);
		if (expectedOutputDir.exists())
		{
			assertTrue(expectedOutputDir.isDirectory());
			assertTrue(UtilitiesForTesting.clearOutDirectory(expectedOutputDir));
			assertTrue(expectedOutputDir.delete());
			assertFalse(expectedOutputDir.exists());
		}
		return expectedOutputDir;
	}




	private void verifyBrowserMediaCreatedCorrectly(File expectedOutputDir, List<File> outputMedia)
	{
		assertEquals(1, outputMedia.size());
		assertEquals(expectedOutputDir, outputMedia.get(0));
		List<String> listOfFileNames = Arrays.asList(expectedOutputDir.list());
		assertTrue(listOfFileNames.size() > 8); 
		assertTrue(listOfFileNames.contains("image.png"));
		assertTrue(listOfFileNames.contains("image_un.png"));
		assertTrue(listOfFileNames.contains("image_text.png"));
		assertTrue(listOfFileNames.contains("image_text_un.png"));
		assertTrue(listOfFileNames.contains("text.png"));
		assertTrue(listOfFileNames.contains("text_un.png"));
		assertTrue(listOfFileNames.contains("frame0000.png"));
	}

	private List<File> testARandomToolInAPostAnimationHandler(PostProductionHandler handler) throws MediaEncodingException
	{
		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		String toolName = WHOMBO_TOOL_1;

		//PostProductionHandler.debugWriteOutAllImagesInCapFile(capFile, new File("./test/"));
		
		assertTrue(capFile.exists());

		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());
		Date datePlusFifteen = new Date(date.getTime() + 15 * 1000); // plus
																		// fifteen
																		// seconds

		ToolUsage testToolUsage = makeToolUsage(datePlusFifteen, toolName);
		
		handler.loadFile(new FileDateStructs(capFile, date));

		

		List<File> mediaOutputs = handler.extractMediaForToolUsage(testToolUsage);

		return mediaOutputs;
	}

	@Test
	public void testSingleToolUsageExtractionReallyEarly() throws Exception
	{
		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		assertTrue(capFile.exists());
		
		String toolName = "WhomboTool #2";
		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());
		Date datePlusOne = new Date(date.getTime() + 1 * 1000); // plus one
																// second

		ToolUsage testToolUsage = makeToolUsage(datePlusOne, toolName);
		
		
		File expectedOutputDir = prepareForBrowserMediaTest(testToolUsage);


		PostProductionHandler handler = makeBrowserMediaPostProductionHandler();
		handler.loadFile(new FileDateStructs(capFile, date));

		

		List<File> mediaOutputs = handler.extractMediaForToolUsage(testToolUsage);

		verifyBrowserMediaCreatedCorrectly(expectedOutputDir, mediaOutputs);
	}

	@Test
	public void testSingleToolUsageExtractionOverlappingFiles() throws Exception
	{		
		File firstcapFile = new File("./src/ForTesting/oneMinuteCap.cap");
		File secondCapFile = new File("./src/ForTesting/oneMinuteCap.cap"); // we'll just reuse this for testing
		assertTrue(firstcapFile.exists());
		assertTrue(secondCapFile.exists());
		
		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());
		Date secondDate = UtilitiesForTesting.truncateTimeToMinute(new Date(date.getTime() + 61 * 1000));

		Date datePlusFiftyFive = new Date(date.getTime() + 55 * 1000); // plus 55 seconds, plenty to over run this file

		String toolName = "WhomboTool #3";
		ToolUsage testToolUsage = makeToolUsage(datePlusFiftyFive, toolName, 10 * 1000);
		
		File expectedOutputDir = prepareForBrowserMediaTest(testToolUsage);
		
		PostProductionHandler handler = makeBrowserMediaPostProductionHandler();
		
		handler.loadFile(new FileDateStructs(firstcapFile, date));
		handler.enqueueOverLoadFile(new FileDateStructs(secondCapFile, secondDate));

		List<File> mediaOutputs = handler.extractMediaForToolUsage(testToolUsage);

		verifyBrowserMediaCreatedCorrectly(expectedOutputDir, mediaOutputs);

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

	private PostProductionHandler makeBrowserMediaPostProductionHandler()
	{
		PostProductionHandler handler = new PostProductionHandler();
		handler.addNewPreAnimationMediaOutput(new PreAnimationImagesToBrowserAnimatedPackage());
		return handler;
	}

}
