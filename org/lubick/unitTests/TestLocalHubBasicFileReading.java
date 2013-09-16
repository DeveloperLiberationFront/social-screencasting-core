package org.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lubick.localHub.LoadedFileEvent;
import org.lubick.localHub.LoadedFileListener;
import org.lubick.localHub.LocalHub;
import org.lubick.localHub.ParsedFileEvent;
import org.lubick.localHub.ParsedFileListener;
import org.lubick.localHub.forTesting.LocalHubDebugAccess;
import org.lubick.localHub.forTesting.TestUtilities;
import org.lubick.localHub.forTesting.ToolStream;

public class TestLocalHubBasicFileReading {

	private static final String LOCAL_HUB_MONITOR_LOCATION = "HF/";
	private static final long MILLIS_IN_DAY = 86400000l;
	private static LocalHubDebugAccess localHub;
	private static File testPluginDirectory;
	//This won't work in the year 2100 or later.  
	private SimpleDateFormat sdf = new SimpleDateFormat("DDDYYkkmm");
	
	//used with listeners.  These give listeners a place to refer
	private LoadedFileEvent observedEvent = null;
	private boolean hasSeenResponseFlag;
	
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
		//create the testPlugin directory to simulate a plugin making something
		testPluginDirectory = new File(LOCAL_HUB_MONITOR_LOCATION+"TestPlugin/");
		if (!testPluginDirectory.exists() && !testPluginDirectory.mkdir())
		{
			fail("Could not create plugin directory");
		}
		
	}

	

	@Before
	public void setUp() throws Exception 
	{
		
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
		File createdNestedFile = TestUtilities.createAbsoluteFileWithContent(nestedDirectory.getAbsolutePath(),"TestPlugin"+sdf.format(currentTime)+".log","ThisIsAToolstream");
		
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
	private void createToolStreamFileAndVerifyItHappened(String fileContents, Date timeStamp, LoadedFileListener loadedFileListener) throws Exception
	{
		assertTrue(localHub.isRunning());
		localHub.addLoadedFileListener(loadedFileListener);
		observedEvent  = null;
			
		File createdFile = TestUtilities.createAbsoluteFileWithContent(testPluginDirectory.getAbsolutePath(),"TestPlugin"+sdf.format(timeStamp)+".log",fileContents);
		
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
	}
	

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
		
		ToolStream ts = ToolStream.generateRandomToolStream(2);
		
		localHub.addParsedFileListener(new ParsedFileListener(){

			@Override
			public void parsedFile(ParsedFileEvent e) {
				// TODO Auto-generated method stub
			}
			
		});
		
		createToolStreamFileAndVerifyItHappened(ts.toJSON(), currentTime, defaultLoadedFileListener);
		
		fail("Not yet implemented");
	}


	/**
	 * Creates a date that is in the future.  The first time this is called, the time 
	 * will be transformed 24 hours into the future, the second time, 48 hours and so on.
	 * @return future time as java.util.Date
	 */
	private static Date getFastForwardedDate() {
		Date rightNow = new Date();
		Date retVal = new Date(rightNow.getTime() + currentFastForwardTime);
		currentFastForwardTime += MILLIS_IN_DAY;
		return retVal;
	}
}
