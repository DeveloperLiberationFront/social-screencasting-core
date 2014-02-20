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
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
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



	private File prepareForBrowserMediaTest(ToolUsage testToolUsage)
	{
		String mediaDirName = FileUtilities.makeFileNameStemForToolPluginMedia(testToolUsage);
		File expectedOutputDir = new File(mediaDirName);
		if (expectedOutputDir.exists())
		{
			assertTrue(expectedOutputDir.isDirectory());
			assertTrue(UtilitiesForTesting.clearOutDirectory(expectedOutputDir));
			assertTrue(expectedOutputDir.delete());
			assertFalse(expectedOutputDir.exists());
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
			}
		}
		return expectedOutputDir;
	}
	//
	//
	//
	//
	private void verifyBrowserMediaCreatedCorrectly(File expectedOutputDir, File folderContainingBrowserPackage)
	{
		assertNotNull(folderContainingBrowserPackage);
		assertTrue(folderContainingBrowserPackage.exists());
		assertTrue(folderContainingBrowserPackage.isDirectory());
		List<String> listOfFileNames = Arrays.asList(expectedOutputDir.list());
		assertTrue(listOfFileNames.size() > 8); 
		assertTrue(listOfFileNames.contains("image.png"));
		assertTrue(listOfFileNames.contains("image_un.png"));
		assertTrue(listOfFileNames.contains("image_text.png"));
		assertTrue(listOfFileNames.contains("image_text_un.png"));
		assertTrue(listOfFileNames.contains("text.png"));
		assertTrue(listOfFileNames.contains("text_un.png"));
		assertTrue(listOfFileNames.contains("frame0000.jpg"));
	}


	@Test
	public void testBasicBrowserPackageExtraction() throws Exception
	{
		PostProductionHandler pph = new PostProductionHandler(new File("./test/"));

		ToolUsage testUsage = makeToolUsage(new Date(7500L), WHOMBO_TOOL_1, 5500);

		File expectedOutputDir = prepareForBrowserMediaTest(testUsage);

		File folderContainingBrowserPackage = pph.extractBrowserMediaForToolUsage(testUsage);

		verifyBrowserMediaCreatedCorrectly(expectedOutputDir, folderContainingBrowserPackage);

		assertEquals(25+28+5+5+6, expectedOutputDir.list().length);		//25 frames (5 seconds) runup, 
		//28 frames, plus 5 copies of the last, plus 5 frames of black and 6 animations

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

}
