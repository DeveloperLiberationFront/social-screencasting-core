package org.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lubick.localHub.LoadedFileEvent;
import org.lubick.localHub.LoadedFileListener;
import org.lubick.localHub.LocalHub;
import org.lubick.localHub.ParsedFileEvent;
import org.lubick.localHub.ParsedFileListener;
import org.lubick.localHub.ToolStream;
import org.lubick.localHub.forTesting.IdealizedToolStream;
import org.lubick.localHub.forTesting.LocalHubDebugAccess;
import org.lubick.localHub.forTesting.TestUtilities;

public class TestLocalHubBasicFileReading {

	private static final String LOCAL_HUB_MONITOR_LOCATION = "HF/";
	private static final long MILLIS_IN_DAY = 86400000l;
	private static LocalHubDebugAccess localHub;
	private File testPluginDirectory;
	//This won't work in the year 2100 or later.  
	private SimpleDateFormat sdf = new SimpleDateFormat("DDDyykkmm");
	
	//used with listeners.  These give listeners a place to refer
	private LoadedFileEvent observedEvent = null;
	private boolean hasSeenResponseFlag = false;
	
	private static long currentFastForwardTime = MILLIS_IN_DAY;
	
	private LoadedFileListener defaultLoadedFileListener = new LoadedFileListener(){
		@Override
		public int loadFileResponse(LoadedFileEvent e) 
		{
			observedEvent = e;
			hasSeenResponseFlag = true;
			return LoadedFileListener.NO_COMMENT;
		}
	};
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
		//Clear out the testing monitor location
		assertTrue(TestUtilities.clearOutDirectory(LOCAL_HUB_MONITOR_LOCATION));
		//start the server
		localHub = LocalHub.startServerAndReturnDebugAccess(LOCAL_HUB_MONITOR_LOCATION);
	
		
	}
	
	
	private static int testIteration = 1;
	public static String getCurrentPluginName()
	{
		return "TestPlugin" + testIteration;
	}
	
	private static void goToNextTest()
	{
		testIteration++;
	}

	

	@Before
	public void setUp() throws Exception 
	{
		//This allows each test to have a different plugin Directory to do things with
		testPluginDirectory = new File(LOCAL_HUB_MONITOR_LOCATION+getCurrentPluginName()+"/");
		if (!testPluginDirectory.exists() && !testPluginDirectory.mkdir())
		{
			fail("Could not create plugin directory");
		}
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
		//Waits in the listener for the response
		observedEvent = null;
		hasSeenResponseFlag = false;
		
		createToolStreamFileAndVerifyItHappened("ThisIsAToolStream", new Date(), defaultLoadedFileListener);
		
		//==========================================
		//Clear out for the adding of the nested directory
		hasSeenResponseFlag = false;
		observedEvent = null;
		
		//manually add the listener
		localHub.addLoadedFileListener(defaultLoadedFileListener);
		
		File nestedDirectory = new File(testPluginDirectory, "DeepNested");
		assertTrue(nestedDirectory.mkdir());
		
		Date currentTime = new Date();
		File createdNestedFile = TestUtilities.createAbsoluteFileWithContent(nestedDirectory.getAbsolutePath(),getCurrentPluginName()+"."+sdf.format(currentTime)+".log","ThisIsAToolstream");
		
		assertNotNull(createdNestedFile);
		assertTrue(createdNestedFile.exists());
		
		int timeCounter = 0;
		while (!hasSeenResponseFlag && timeCounter < 3) 
		{
			Thread.sleep(1000);
			timeCounter++;
		}
		//This is a deeply nested folder, should not be read
		assertFalse(hasSeenResponseFlag);
		assertNull(observedEvent);
		
		localHub.removeLoadedFileListener(defaultLoadedFileListener);
	}
	
	
	/**
	 * Provides a way to create a file and have a listener respond to it.
	 * 
	 * After this subtest passes, the listener is removed
	 * @param fileContents
	 * @param loadedFileListener
	 * @param timeStamp 
	 * @throws InterruptedException
	 */
	private File createToolStreamFileAndVerifyItHappened(String fileContents, Date timeStamp, LoadedFileListener loadedFileListener) throws Exception
	{
		assertTrue(localHub.isRunning());
		localHub.addLoadedFileListener(loadedFileListener);
		observedEvent  = null;
			
		File createdFile = TestUtilities.createAbsoluteFileWithContent(testPluginDirectory.getAbsolutePath(),getCurrentPluginName()+"."+sdf.format(timeStamp)+".log",fileContents);
		
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
	

	private boolean hasParsedFlag = false;
	private ParsedFileEvent parsedEvent = null;
	@Test
	public void testReadingInToolStreamAndParsing() throws Exception
	{
		assertTrue(localHub.isRunning());
		//Waits in the listener for the response
		observedEvent = null;
		hasSeenResponseFlag = false;
		//Has the event
		
		//This test happens in the future
		Date currentTime = getFastForwardedDate();
		
		IdealizedToolStream ts = IdealizedToolStream.generateRandomToolStream(2);
		
		ParsedFileListener parsedFileListener = new ParsedFileListener(){

			@Override
			public void parsedFile(ParsedFileEvent e) {
				hasParsedFlag = true;
				parsedEvent = e;
			}
			
		};
		
		localHub.addParsedFileListener(parsedFileListener);
		
		createToolStreamFileAndVerifyItHappened(ts.toJSON(), currentTime, defaultLoadedFileListener);
		
		//Our tool stream should not have been parsed yet.
		assertFalse(hasParsedFlag);
		assertNull(parsedEvent);
		
		//This should be into the next minute, so the date string will be different
		Date futureTime = new Date(currentTime.getTime() + (1* 60*1000l));
		IdealizedToolStream newTS = IdealizedToolStream.generateRandomToolStream(5);
		
		//reset these to go
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
			assertEquals(getCurrentPluginName(),parsedEvent.getPluginName());
			assertEquals(TestUtilities.truncateTimeToMinute(currentTime), parsedEvent.getFileTimeStamp());
	
		}
		else 
		{
			fail("test ParsingFile has timed out");
		}
		
		localHub.removeParsedFileListener(parsedFileListener);
	}
	
	@Test
	public void testSeveralMinutesWorthOfDataAndExit() throws Exception 
	{
		//We'll be making 3 files to simulate multiple things
		Date currentTime = getFastForwardedDate();
		Date teePlusOne = new Date(currentTime.getTime() + 60*1000);
		Date teePlusTwo = new Date(teePlusOne.getTime() + 60*1000);
		Date teePlusThree = new Date(teePlusTwo.getTime() + 60*1000);
		
		IdealizedToolStream firstToolStream = IdealizedToolStream.generateRandomToolStream(30);
		IdealizedToolStream secondToolStream = IdealizedToolStream.generateRandomToolStream(40);
		IdealizedToolStream thirdToolStream = IdealizedToolStream.generateRandomToolStream(50);
		
		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened(firstToolStream.toJSON(), currentTime, defaultLoadedFileListener);
		
		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened(secondToolStream.toJSON(), teePlusOne, defaultLoadedFileListener);
		
		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened(thirdToolStream.toJSON(), teePlusTwo, defaultLoadedFileListener);
		
		observedEvent = null;
		hasSeenResponseFlag = false;
		createToolStreamFileAndVerifyItHappened("", teePlusThree, defaultLoadedFileListener);
		
		Thread.sleep(1000);
		//After waiting 1 second, the streams should be parsed
		List<ToolStream.ToolUsage> allHistoriesOfToolUsages = localHub.getAllToolUsageHistoriesForPlugin(getCurrentPluginName());
		
		assertEquals(allHistoriesOfToolUsages.size(), 120);
	}


	/**
	 * Creates a date that is in the future.  The first time this is called, the time 
	 * will be transformed 24 hours into the future, the second time, 48 hours and so on.
	 * @return future time as java.util.Date
	 */
	private static Date getFastForwardedDate() 
	{
		Date rightNow = new Date();
		Date retVal = new Date(rightNow.getTime() + currentFastForwardTime);
		currentFastForwardTime += MILLIS_IN_DAY;
		return retVal;
	}
}
