package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.util.FileUtilities;

public class TestPostProductionHandler
{

	private static final int EXTRA_FRAMES = 5+5;   //5 copies of the last, plus 5 frames of black
	private static final int NUMBER_KEYBOARD_ANIMATIONS = 6;  //and 6 animations
	private static final int EXTRA_FRAMES_AND_ANIMATIONS = EXTRA_FRAMES + NUMBER_KEYBOARD_ANIMATIONS;  
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
		TestingUtils.makeSureLoggingIsSetUp();
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




	private UserManager fakeUserManager;
	
	@Before
	public void setUp()
	{
		this.fakeUserManager = new UnitTestUserManager("Test User", "test@mailinator.com", "123");
	}
	



	@Test
	public void testBasicBrowserPackageExtraction() throws Exception
	{
		Date screencastBeginDate = new Date(60_000); //one minute past the epoch
		
		PostProductionHandler pph = standardFolderAndProductionSetup(screencastBeginDate);
	
		ToolUsage testUsage = makeKeyboardToolUsage(new Date(screencastBeginDate.getTime() + 7500L), WHOMBO_TOOL_1, 5500);
	
		File expectedOutputDir = prepareForBrowserMediaTest(testUsage);
	
		File folderContainingBrowserPackage = pph.extractBrowserMediaForToolUsage(testUsage);
	
		//25 frames (5 seconds) runup, 28 frames, plus extra
		verifyBrowserMediaCreatedCorrectlyKeyboard(expectedOutputDir, folderContainingBrowserPackage, 25 + 28 + EXTRA_FRAMES_AND_ANIMATIONS);	
	
	}
	
	@Test
	public void testBasicBrowserPackageExtractionGUI() throws Exception
	{
		Date screencastBeginDate = new Date(60_000); //one minute past the epoch
		
		PostProductionHandler pph = standardFolderAndProductionSetup(screencastBeginDate);
	
		ToolUsage testUsage = makeGUIToolUsage(new Date(screencastBeginDate.getTime() + 8500L), WHOMBO_TOOL_1, 5500);
				
		File expectedOutputDir = prepareForBrowserMediaTest(testUsage);
	
		File folderContainingBrowserPackage = pph.extractBrowserMediaForToolUsage(testUsage);
	
		//25 frames (5 seconds) runup, 28 frames, plus extra frames (no animations because GUI)
		verifyBrowserMediaCreatedCorrectlyGUI(expectedOutputDir, folderContainingBrowserPackage, 25 + 28 + EXTRA_FRAMES);	
	
	}
	
	@Test
	public void testBrowserPackageExtractionWAYBeforeScreencasting() throws Exception
	{
		Date screencastBeginDate = new Date(60_000); //one minute past the epoch
		
		PostProductionHandler pph = standardFolderAndProductionSetup(screencastBeginDate);
	
		ToolUsage testUsage = makeKeyboardToolUsage(new Date(0), WHOMBO_TOOL_1, 5500);
	
		File expectedOutputDir = prepareForBrowserMediaTest(testUsage);
		
		assertNotNull(expectedOutputDir);
		assertNull(pph.extractBrowserMediaForToolUsage(testUsage));
	
	}
	
	@Test
	public void testBrowserPackageExtractionSomewhatBeforeScreencasting() throws Exception
	{
		Date screencastBeginDate = new Date(60_000); //one minute past the epoch
		
		PostProductionHandler pph = standardFolderAndProductionSetup(screencastBeginDate);
	
		ToolUsage testUsage = makeKeyboardToolUsage(new Date(screencastBeginDate.getTime() - 4000L), WHOMBO_TOOL_1, 5500);
	
		File expectedOutputDir = prepareForBrowserMediaTest(testUsage);
		
		assertNotNull(expectedOutputDir);
		assertNull(pph.extractBrowserMediaForToolUsage(testUsage));
	
	}
	
	@Test
	public void testBrowserPackageExtractionCloseToBeginningScreencasting() throws Exception
	{
		Date screencastBeginDate = new Date(60_000); //one minute past the epoch
		
		PostProductionHandler pph = standardFolderAndProductionSetup(screencastBeginDate);
	
		ToolUsage testUsage = makeKeyboardToolUsage(new Date(screencastBeginDate.getTime() + 1000L), WHOMBO_TOOL_1, 5500);
	
		File expectedOutputDir = prepareForBrowserMediaTest(testUsage);
	
		File folderContainingBrowserPackage = pph.extractBrowserMediaForToolUsage(testUsage);
	
		//6 frames (~1 second) runup, 28 frames, plus extra
		verifyBrowserMediaCreatedCorrectlyKeyboard(expectedOutputDir, folderContainingBrowserPackage, 6 + 28 + EXTRA_FRAMES_AND_ANIMATIONS);	
	
	}
	
	




	private PostProductionHandler standardFolderAndProductionSetup(Date screencastBeginDate)
	{
		setUpScreencastingFolderForDate(screencastBeginDate);
		
		PostProductionHandler pph = new PostProductionHandler(TEST_SCREENCAST_FOLDER, fakeUserManager);
		return pph;
	}



	private void setUpScreencastingFolderForDate(Date screencastBeginDate)
	{
		if (timeAssociatedWithScreencasting == null)
		{
			logger.info("Checking to see if we need to re-unzip the screencasts");
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
		
		//TestingUtils.clearOutDirectory(TEST_SCREENCAST_FOLDER);
		//TestingUtils.unzipFileToFolder(DUMMY_SCREENCAST_ZIP, TEST_SCREENCAST_FOLDER);
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
		String mediaDirName = testToolUsage.getUniqueIdentifier(fakeUserManager.getUserEmail());
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
				logger.info("Interrupted while waiting for prepared browser media folder to be ready",e);
			}
		}
		return expectedOutputDir;
	}

	private void verifyBrowserMediaCreatedCorrectlyKeyboard(File expectedOutputDir, File folderContainingBrowserPackage, int expectedNumFrames)
	{
		verifyBrowserMediaCreatedCorrectlyGUI(expectedOutputDir, folderContainingBrowserPackage, expectedNumFrames);
		List<String> listOfFileNames = Arrays.asList(expectedOutputDir.list());
		assertTrue(listOfFileNames.contains("image.png"));
		assertTrue(listOfFileNames.contains("image_un.png"));
		assertTrue(listOfFileNames.contains("image_text.png"));
		assertTrue(listOfFileNames.contains("image_text_un.png"));
		assertTrue(listOfFileNames.contains("text.png"));
		assertTrue(listOfFileNames.contains("text_un.png"));
		assertTrue(listOfFileNames.contains("frame0000.jpg"));	
	}
	
	private void verifyBrowserMediaCreatedCorrectlyGUI(File expectedOutputDir, File folderContainingBrowserPackage, int expectedNumFrames)
	{
		assertNotNull(folderContainingBrowserPackage);
		assertTrue(folderContainingBrowserPackage.exists());
		assertTrue(folderContainingBrowserPackage.isDirectory());
		List<String> listOfFileNames = Arrays.asList(expectedOutputDir.list());
		assertEquals(expectedNumFrames, listOfFileNames.size()); 
		assertTrue(listOfFileNames.contains("frame0000.jpg"));
		verifyFrameNameIntegrity(listOfFileNames, listOfFileNames.size() - NUMBER_KEYBOARD_ANIMATIONS);
		
	}


	private void verifyFrameNameIntegrity(List<String> listOfFileNames, int expectedFrames)
	{
		Collections.sort(listOfFileNames);
		for(int i = 0;i<expectedFrames;i++)
		{
			assertEquals("frame"+FileUtilities.padIntTo4Digits(i)+"."+PostProductionHandler.INTERMEDIATE_FILE_FORMAT, listOfFileNames.get(i));
		}
		
	}




	public static ToolUsage makeKeyboardToolUsage(Date toolUsageDate, String toolUsageName, int duration)
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(TestingUtils.truncateTimeToMinute(toolUsageDate));
		iToolStream.addToolUsage(toolUsageName, DEFAULT_TESTING_TOOL_CLASS, DEFAULT_TESTING_KEYPRESS, toolUsageDate, duration);

		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin(TEST_PLUGIN_NAME);
		assertEquals(1, toolStream.getAsList().size());

		ToolUsage testToolUsage = toolStream.getAsList().get(0);
		return testToolUsage;
	}




	public static ToolUsage makeGUIToolUsage(Date toolUsageDate, String toolUsageName, int duration)
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(TestingUtils.truncateTimeToMinute(toolUsageDate));
		iToolStream.addToolUsage(toolUsageName, DEFAULT_TESTING_TOOL_CLASS, ToolStream.MENU_KEY_PRESS, toolUsageDate, duration);

		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin(TEST_PLUGIN_NAME);
		assertEquals(1, toolStream.getAsList().size());

		ToolUsage testToolUsage = toolStream.getAsList().get(0);
		return testToolUsage;
	}

}
