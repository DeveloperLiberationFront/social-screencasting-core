package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.Runner;
import edu.ncsu.lubick.localHub.LoadedFileEvent;
import edu.ncsu.lubick.localHub.LoadedFileListener;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ParsedFileEvent;
import edu.ncsu.lubick.localHub.ParsedFileListener;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.database.LocalSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.LocalHubDebugAccess;
import edu.ncsu.lubick.localHub.forTesting.UtilitiesForTesting;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.util.FileUtilities;

public class TestLocalHubBasicFileReading {
	
	static
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
	}

	private static Logger logger = Logger.getLogger(TestLocalHubBasicFileReading.class.getName());
	private static final String LOCAL_HUB_MONITOR_LOCATION = "BasicFileReading/";
	private static final long MILLIS_IN_DAY = 86400000L;
	private static int testIteration = 1;
	private static LocalHubDebugAccess localHub;
	private File testPluginDirectory;
	// used with listeners. These give listeners a place to refer
	private LoadedFileEvent observedEvent = null;
	private boolean hasSeenResponseFlag = false;

	private static long currentFastForwardTime = MILLIS_IN_DAY;

	// used in testReadingInToolStreamAndParsing()
	private boolean hasParsedFlag = false;
	private ParsedFileEvent parsedEvent = null;

	private LoadedFileListener defaultLoadedFileListener = new LoadedFileListener() {
		@Override
		public int loadFileResponse(LoadedFileEvent e)
		{
			observedEvent = e;
			hasSeenResponseFlag = true;
			return LoadedFileListener.NO_COMMENT;
		}
	};

	private ParsedFileListener defaultParsedFileListener = new ParsedFileListener() {

		@Override
		public void parsedFile(ParsedFileEvent e)
		{
			hasParsedFlag = true;
			parsedEvent = e;
		}

	};

	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		RemoteSQLDatabaseFactory.setUpToUseMockDB(true);
		// Clear out the testing monitor location
		assertTrue(UtilitiesForTesting.clearOutDirectory(LOCAL_HUB_MONITOR_LOCATION));
		startLocalHubWithClearDatabase();

	}

	@AfterClass
	public static void shutDownAll() throws Exception
	{
		logger.info("Shutting down and clearing out all evidence");
		localHub.shutDown();
		assertTrue(UtilitiesForTesting.clearOutDirectory(LOCAL_HUB_MONITOR_LOCATION));
		File f = new File(LOCAL_HUB_MONITOR_LOCATION);
		f.deleteOnExit();
		while (localHub.isRunning())
		{
			Thread.sleep(500);
		}
		assertFalse(localHub.isRunning());
	}

	@Before
	public void setUp() throws Exception
	{
		// This allows each test to have a different plugin Directory to do
		// things with
		makeTestPluginDirectory();
	}

	@After
	public void tearDown() throws Exception
	{
		goToNextTest();
	}

	@Test
	public void testReadingInFileAndIgnoringNestedFolder() throws Exception
	{
		assertTrue(localHub.isRunning());
		// Waits in the listener for the response
		observedEvent = null;
		hasSeenResponseFlag = false;

		createToolStreamFileAndVerifyItHappened("ThisIsAToolStream", new Date(), defaultLoadedFileListener);

		// ==========================================
		// Clear out for the adding of the nested directory
		hasSeenResponseFlag = false;
		observedEvent = null;

		// manually add the listener
		localHub.addLoadedFileListener(defaultLoadedFileListener);

		File nestedDirectory = new File(testPluginDirectory, "DeepNested");
		assertTrue(nestedDirectory.mkdir());

		Date currentTime = new Date();
		
		File createdNestedFile = UtilitiesForTesting.createAbsoluteFileWithContent(nestedDirectory.getAbsolutePath(), 
				FileUtilities.encodeLogFileName(getCurrentPluginName(), currentTime), "ThisIsAToolstream");

		assertNotNull(createdNestedFile);
		assertTrue(createdNestedFile.exists());

		int timeCounter = 0;
		while (!hasSeenResponseFlag && timeCounter < 3)
		{
			Thread.sleep(1000);
			timeCounter++;
		}
		// This is a deeply nested folder, should not be read
		assertFalse(hasSeenResponseFlag);
		assertNull(observedEvent);

		localHub.removeLoadedFileListener(defaultLoadedFileListener);
	}

	@Test
	public void testReadingInToolStreamAndParsing() throws Exception
	{
		assertTrue(localHub.isRunning());
		// Waits in the listener for the response
		observedEvent = null;
		hasSeenResponseFlag = false;
		// Has the event

		// This test happens in the future
		Date currentTime = getFastForwardedDate();

		IdealizedToolStream ts = IdealizedToolStream.generateRandomToolStream(2, currentTime);

		File toolStream = createToolStreamAndVerifyItWasParsed(ts, currentTime);

		assertFalse(toolStream.exists());

		List<String> pluginNames = localHub.getAllPluginNames();
		logger.debug(pluginNames);
		assertTrue(pluginNames.contains(getCurrentPluginName()));

		localHub.removeParsedFileListener(defaultParsedFileListener);
	}

	@Test
	public void testParsingFilesFromPrexistingPlugin() throws Exception
	{
		shutDownAll();
		// 1 second sleep to make sure everything shuts down okay
		Thread.sleep(1000);

		makeTestPluginDirectory();
		Date currentTime = getFastForwardedDate();
		Date timeInPast = new Date(currentTime.getTime() - 60 * 60 * 1000); // one
																			// hour
																			// ago
		IdealizedToolStream pastToolStream = IdealizedToolStream.generateRandomToolStream(20, timeInPast);

		File oldToolStream = createToolStreamOnDisk(pastToolStream);

		startLocalHubWithClearDatabase();

		IdealizedToolStream newToolStream = IdealizedToolStream.generateRandomToolStream(40, currentTime);

		File secondToolStream = createToolStreamAndVerifyItWasParsed(newToolStream, currentTime);

		List<ToolStream.ToolUsage> allHistoriesOfToolUsages = localHub.getAllToolUsageHistoriesForPlugin(getCurrentPluginName());

		assertNotNull(allHistoriesOfToolUsages);
		assertEquals(60, allHistoriesOfToolUsages.size());

		assertFalse(oldToolStream.exists());
		assertFalse(secondToolStream.exists());

	}

	@Test
	public void testSeveralMinutesWorthOfDataAndExit() throws Exception
	{
		assertTrue(localHub.isRunning());
		// We'll be making 3 files to simulate multiple things
		Date currentTime = getFastForwardedDate();
		Date teePlusOne = new Date(currentTime.getTime() + 60 * 1000);
		Date teePlusTwo = new Date(teePlusOne.getTime() + 60 * 1000);
		Date teePlusThree = new Date(teePlusTwo.getTime() + 60 * 1000);

		IdealizedToolStream firstToolStream = IdealizedToolStream.generateRandomToolStream(30, currentTime);
		IdealizedToolStream secondToolStream = IdealizedToolStream.generateRandomToolStream(40, teePlusOne);
		IdealizedToolStream thirdToolStream = IdealizedToolStream.generateRandomToolStream(50, teePlusTwo);

		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened(firstToolStream, defaultLoadedFileListener);

		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened(secondToolStream, defaultLoadedFileListener);

		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened(thirdToolStream, defaultLoadedFileListener);

		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened("", teePlusThree, defaultLoadedFileListener);

		List<ToolStream.ToolUsage> allHistoriesOfToolUsages = localHub.getAllToolUsageHistoriesForPlugin(getCurrentPluginName());

		assertNotNull(allHistoriesOfToolUsages);
		assertEquals(120, allHistoriesOfToolUsages.size());

		for (ToolStream.ToolUsage tu : allHistoriesOfToolUsages)
		{
			assertNotNull(tu.getTimeStamp());
			assertNotNull(tu.getToolClass());
			assertNotNull(tu.getToolKeyPresses());
			assertNotNull(tu.getToolName());
			assertEquals(getCurrentPluginName(), tu.getPluginName());

		}
	}

	@Test
	//TODO change to Browser package
	public void testDatabasePullAndVideoCreation() throws Exception
	{
		assertTrue(localHub.isRunning());
		Date currentTime = getFastForwardedDate();
		Date teeMinusFive = new Date(currentTime.getTime() - 5 * 1000);
		Date teePlusThirty = new Date(currentTime.getTime() + 30 * 1000);
		Date teePlusOneM = new Date(currentTime.getTime() + 60 * 1000);

		IdealizedToolStream firstToolStream = IdealizedToolStream.generateRandomToolStream(30, currentTime);
		String uniqueToolString = "My.Unique.Tool.name";
		firstToolStream.addToolUsage(uniqueToolString, "Special", "Ctrl + 7,5,2", teePlusThirty, 10 * 1000);

		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened(firstToolStream, defaultLoadedFileListener);

		String nameOfSourceFile = FileUtilities.encodeCapFileName(teeMinusFive);
		copyScreenCastCapFileToDirectoryAndVerifyItHappened(new File("./src/ForTesting/oneMinuteCap.cap"), nameOfSourceFile);

		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened("", teePlusOneM, defaultLoadedFileListener);

		List<ToolStream.ToolUsage> allHistoriesOfToolUsages = localHub.getAllToolUsageHistoriesForPlugin(getCurrentPluginName());

		assertNotNull(allHistoriesOfToolUsages);
		assertEquals(31, allHistoriesOfToolUsages.size());

		File outputFile = getJustVideoFromLocalHub(uniqueToolString);

		assertNotNull(outputFile);
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		//assertTrue(outputFile.getName().endsWith(ImagesWithAnimationToVideoOutput.VIDEO_EXTENSION));
		assertTrue(outputFile.length() > 100000); // I expect the file size to
													// be at least 100k and no
													// more than 3Mb
		assertTrue(outputFile.length() < 3000000);

	}

	public File getJustVideoFromLocalHub(String uniqueToolString) throws MediaEncodingException
	{
		return null;
		//TODO migrate to non-video
		/*List<File> mediaOutputs = localHub.extractVideoForLastUsageOfTool(getCurrentPluginName(), uniqueToolString);
		if (mediaOutputs == null)
		{
			return null;
		}
		for (File f : mediaOutputs)
		{
			if (f.getName().endsWith(ImagesWithAnimationToVideoOutput.VIDEO_EXTENSION))
				return f;
		}
		return null;
		*/
	}

	/**
	 * Provides a way to create a file and have a listener respond to it.
	 * 
	 * After this subtest passes, the listener is removed
	 * 
	 * @param fileContents
	 * @param loadedFileListener
	 * @param timeStamp
	 * @throws InterruptedException
	 */
	private File createToolStreamFileAndVerifyItHappened(String fileContents, Date timeStamp, LoadedFileListener loadedFileListener) throws Exception
	{
		assertTrue(localHub.isRunning());
		localHub.addLoadedFileListener(loadedFileListener);
		observedEvent = null;

		File createdFile = UtilitiesForTesting.createAbsoluteFileWithContent(testPluginDirectory.getAbsolutePath(),
				FileUtilities.encodeLogFileName(getCurrentPluginName(), timeStamp), fileContents);

		assertNotNull(createdFile);
		assertTrue(createdFile.exists());

		int timeCounter = 0;
		while (!hasSeenResponseFlag && timeCounter < 5)
		{
			Thread.sleep(1000);
			timeCounter++;
		}
		if (timeCounter <= 5 && observedEvent != null)
		{
			assertEquals(createdFile.getName(), observedEvent.getFileName());
			assertEquals(fileContents, observedEvent.getFileContents());
			assertFalse(observedEvent.wasInitialReadIn());
		}
		else
		{
			localHub.removeLoadedFileListener(loadedFileListener);
			fail("test ReadingInFile has timed out");
		}

		localHub.removeLoadedFileListener(loadedFileListener);

		return createdFile;
	}

	private File createToolStreamFileAndVerifyItHappened(IdealizedToolStream its, LoadedFileListener loadedFileListener) throws Exception
	{
		return createToolStreamFileAndVerifyItHappened(its.toJSON(), its.getTimeStamp(), loadedFileListener);
	}

	private File copyScreenCastCapFileToDirectoryAndVerifyItHappened(File capFile, String newFileName) throws IOException, InterruptedException
	{
		assertTrue(localHub.isRunning());
		localHub.addLoadedFileListener(defaultLoadedFileListener);
		observedEvent = null;

		File createdFile = UtilitiesForTesting.copyFileToFolder(testPluginDirectory.getAbsolutePath(), newFileName, capFile);

		assertNotNull(createdFile);
		assertTrue(createdFile.exists());

		int timeCounter = 0;
		while (!hasSeenResponseFlag && timeCounter < 5)
		{
			Thread.sleep(1000);
			timeCounter++;
		}
		if (timeCounter <= 5 && observedEvent != null)
		{
			assertEquals(createdFile.getName(), observedEvent.getFileName());
			assertFalse(observedEvent.wasInitialReadIn());
		}
		else
		{
			localHub.removeLoadedFileListener(defaultLoadedFileListener);
			fail("test ReadingInFile has timed out");
		}

		localHub.removeLoadedFileListener(defaultLoadedFileListener);

		return createdFile;

	}

	private static String getCurrentPluginName()
	{
		return "TestPlugin" + testIteration;
	}

	/**
	 * Creates a date that is in the future. The first time this is called, the time will be transformed 24 hours into the future, the second time, 48 hours and
	 * so on.
	 * 
	 * @return future time as java.util.Date
	 */
	private static Date getFastForwardedDate()
	{
		Date rightNow = new Date();
		Date retVal = new Date(rightNow.getTime() + currentFastForwardTime);
		currentFastForwardTime += MILLIS_IN_DAY;
		return retVal;
	}

	private static void goToNextTest()
	{
		testIteration++;
	}

	private static void startLocalHubWithClearDatabase()
	{
		File databaseFile = new File(LocalSQLDatabaseFactory.DEFAULT_SQLITE_LOCATION);
		if (databaseFile.exists())
		{
			assertTrue(databaseFile.delete());
		}
		// start the server
		localHub = LocalHub.startServerAndReturnDebugAccess(LOCAL_HUB_MONITOR_LOCATION, false, false);
	}

	private void makeTestPluginDirectory()
	{
		testPluginDirectory = new File(LOCAL_HUB_MONITOR_LOCATION + getCurrentPluginName() + "/");
		if (!testPluginDirectory.exists() && !testPluginDirectory.mkdir())
		{
			fail("Could not create plugin directory");
		}
	}

	private File createToolStreamOnDisk(IdealizedToolStream its)
	{
		return UtilitiesForTesting.createAbsoluteFileWithContent(testPluginDirectory.getAbsolutePath(),
				FileUtilities.encodeLogFileName(getCurrentPluginName(), its.getTimeStamp()), its.toJSON());

	}

	private File createToolStreamAndVerifyItWasParsed(IdealizedToolStream ts, Date baselineDate) throws Exception, InterruptedException
	{

		localHub.addParsedFileListener(defaultParsedFileListener);
		File retVal = createToolStreamFileAndVerifyItHappened(ts, defaultLoadedFileListener);

		// Our tool stream should not have been parsed yet.
		assertFalse(hasParsedFlag);
		assertNull(parsedEvent);

		// This should be into the next minute, so the date string will be
		// different
		Date futureTime = new Date(baselineDate.getTime() + (1 * 60 * 1000L));
		IdealizedToolStream newTS = IdealizedToolStream.generateRandomToolStream(0);

		// reset these to go
		observedEvent = null;
		hasSeenResponseFlag = false;

		createToolStreamFileAndVerifyItHappened(newTS.toJSON(), futureTime, defaultLoadedFileListener);

		int timeCounter = 0;
		while (!hasParsedFlag && timeCounter < 500)
		{
			Thread.sleep(1000);
			timeCounter++;
		}
		if (timeCounter <= 5 && parsedEvent != null)
		{
			assertEquals(ts.toJSON(), parsedEvent.getInputJSON());
			assertTrue(ts.isEquivalent(parsedEvent.getToolStream()));
			assertEquals(getCurrentPluginName(), parsedEvent.getPluginName());
			assertEquals(UtilitiesForTesting.truncateTimeToMinute(baselineDate), parsedEvent.getFileTimeStamp());

		}
		else
		{
			fail("test ParsingFile has timed out");
		}

		return retVal;
	}
}
