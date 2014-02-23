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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.Runner;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.util.FileUtilities;

public class TestPostProductionHandler
{
	private static final int MILLIS_BETWEEN_FRAMES = 200;
	private static final File TEST_SCREENCAST_FOLDER = new File("./test_screencasting/");
	private static final File DUMMY_SCREENCAST_ZIP = new File("sampleScreencastMinute.zip");
	
	private static final String WHOMBO_TOOL_1 = "WhomboTool #1";
	private static final String TEST_PLUGIN_NAME = "Testing";
	private static final String DEFAULT_TESTING_KEYPRESS = "Ctrl+5";
	private static final String DEFAULT_TESTING_TOOL_CLASS = "Debug";
	
	private static Logger logger = null;
	
	private static Date timeAssociatedWithScreencasting = null;

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
		logger = Logger.getLogger(TestPostProductionHandler.class);
		RemoteSQLDatabaseFactory.setUpToUseMockDB(true);
		
		
		if (!TEST_SCREENCAST_FOLDER.exists() && !TEST_SCREENCAST_FOLDER.mkdirs())
		{
			fail("Could not setup "+TEST_SCREENCAST_FOLDER);
		}
		if (!DUMMY_SCREENCAST_ZIP.exists())
		{
			fail("Need a zip file of screencasting frames to perform tests on.  Download from bitbucket and name as "+DUMMY_SCREENCAST_ZIP);
		}
	}
	



	@Test
	public void testBasicBrowserPackageExtraction() throws Exception
	{
		Date screencastBeginDate = new Date(0);
		
		setUpScreencastingFolderForDate(screencastBeginDate);
		
		PostProductionHandler pph = new PostProductionHandler(TEST_SCREENCAST_FOLDER);
	
		ToolUsage testUsage = makeToolUsage(new Date(screencastBeginDate.getTime() + 7500L), WHOMBO_TOOL_1, 5500);
	
		File expectedOutputDir = prepareForBrowserMediaTest(testUsage);
	
		File folderContainingBrowserPackage = pph.extractBrowserMediaForToolUsage(testUsage);
	
		verifyBrowserMediaCreatedCorrectly(expectedOutputDir, folderContainingBrowserPackage);
	
		assertEquals(25+28+5+5+6, expectedOutputDir.list().length);		//25 frames (5 seconds) runup, 
		//28 frames, plus 5 copies of the last, plus 5 frames of black and 6 animations
	
	}



	private void setUpScreencastingFolderForDate(Date screencastBeginDate)
	{
		if (timeAssociatedWithScreencasting == null)
		{
			File[] files = TEST_SCREENCAST_FOLDER.listFiles();
			Arrays.sort(files);
			if (files.length < 300 || !files[0].getName().equals(FileUtilities.encodeMediaFrameName(screencastBeginDate)))
			{
				unzipAndRenameDummyScreencast(screencastBeginDate);
			}
		}
		else if (!timeAssociatedWithScreencasting.equals(screencastBeginDate))
		{
			unzipAndRenameDummyScreencast(screencastBeginDate);
		}
	}




	private void unzipAndRenameDummyScreencast(Date screencastBeginDate)
	{
		logger.info("Replacing test screencasts with renamed sample ones starting at "+screencastBeginDate);
		
		timeAssociatedWithScreencasting = new Date(screencastBeginDate.getTime());	//clone this date as the param will be used below
		
		TestingUtils.clearOutDirectory(TEST_SCREENCAST_FOLDER);
		TestingUtils.unzipFileToFolder(DUMMY_SCREENCAST_ZIP, TEST_SCREENCAST_FOLDER);
		File[] files = TEST_SCREENCAST_FOLDER.listFiles();
		assertTrue(files.length >= 300);
		Arrays.sort(files);
		for(File f:files)
		{
			assertTrue(f.renameTo(new File(TEST_SCREENCAST_FOLDER, FileUtilities.encodeMediaFrameName(screencastBeginDate))));
			screencastBeginDate.setTime(screencastBeginDate.getTime() + MILLIS_BETWEEN_FRAMES);
		}
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			logger.info("Interrupted while waiting for the file system to catch up",e);
		}
	}



	private File prepareForBrowserMediaTest(ToolUsage testToolUsage)
	{
		String mediaDirName = FileUtilities.makeFileNameStemForToolPluginMedia(testToolUsage);
		File expectedOutputDir = new File(mediaDirName);
		if (expectedOutputDir.exists())
		{
			assertTrue(expectedOutputDir.isDirectory());
			assertTrue(TestingUtils.clearOutDirectory(expectedOutputDir));
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
