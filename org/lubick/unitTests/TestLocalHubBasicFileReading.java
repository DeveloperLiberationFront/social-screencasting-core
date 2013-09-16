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
import org.lubick.localHub.forTesting.LocalHubDebugAccess;
import org.lubick.localHub.forTesting.TestUtilities;

public class TestLocalHubBasicFileReading {

	private static final String LOCAL_HUB_MONITOR_LOCATION = "HF/";
	private static LocalHubDebugAccess localHub;
	private static File testPluginDirectory;
	//This won't work in the year 2100 or later.  
	private SimpleDateFormat sdf = new SimpleDateFormat("DDDYYkkmm");
	
	//testReadingInFile()
	private boolean readingInFileHasSeenResponse = false;
	private LoadedFileEvent readingInFileReturnedEvent = null;
	
	//used with listeners.  It needs to be up here as a field because it gets passed in all sorts of method calls
	private LoadedFileEvent observedEvent = null;
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
		readingInFileHasSeenResponse = false;
		
		//Has the event
		readingInFileReturnedEvent = null;
		localHub.addLoadedFileListener(new LoadedFileListener(){
			@Override
			public int loadFileResponse(LoadedFileEvent e) 
			{
				readingInFileReturnedEvent = e;
				readingInFileHasSeenResponse = true;
				return LoadedFileListener.NO_COMMENT;
			}
		});
		
		Date currentTime = new Date();
		
		File createdFile = TestUtilities.createAbsoluteFileWithContent(testPluginDirectory.getAbsolutePath(),"TestPlugin"+sdf.format(currentTime)+".log","ThisIsAToolstream");
		
		assertNotNull(createdFile);
		assertTrue(createdFile.exists());
		
		int timeCounter = 0;
		while (!readingInFileHasSeenResponse && timeCounter < 5) 
		{
			Thread.sleep(1000);
			timeCounter++;
		}
		if (timeCounter <= 5 && readingInFileReturnedEvent != null)
		{
			assertEquals(createdFile.getName(), readingInFileReturnedEvent.getFileName());
			assertEquals("ThisIsAToolstream", readingInFileReturnedEvent.getFileContents());
			assertFalse(readingInFileReturnedEvent.wasInitialReadIn());
		}
		else 
		{
			fail("test ReadingInFile has timed out");
		}
		//==========================================
		//Clear out for the adding of the nested directory
		readingInFileHasSeenResponse = false;
		
		//Has the event
		readingInFileReturnedEvent = null;
		
		File nestedDirectory = new File(testPluginDirectory, "DeepNested");
		assertTrue(nestedDirectory.mkdir());
		
		File createdNestedFile = TestUtilities.createAbsoluteFileWithContent(nestedDirectory.getAbsolutePath(),"TestPlugin"+sdf.format(currentTime)+".log","ThisIsAToolstream");
		
		assertNotNull(createdNestedFile);
		assertTrue(createdNestedFile.exists());
		
		timeCounter = 0;
		while (!readingInFileHasSeenResponse && timeCounter < 3) 
		{
			Thread.sleep(1000);
			timeCounter++;
		}
		//This is a deeply nested folder, should not be read
		assertFalse(readingInFileHasSeenResponse);
		assertNull(readingInFileReturnedEvent);
	}
	
	
	private void createToolStreamFileAndVerifyItHappened(String fileContents, LoadedFileListener loadedFileListener, Boolean hasSeenResponseFlag) throws InterruptedException
	{
		assertTrue(localHub.isRunning());
		localHub.addLoadedFileListener(loadedFileListener);
		observedEvent  = null;
		
		Date currentTime = new Date();
		
		File createdFile = TestUtilities.createAbsoluteFileWithContent(testPluginDirectory.getAbsolutePath(),"TestPlugin"+sdf.format(currentTime)+".log",fileContents);
		
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
			assertEquals("ThisIsAToolstream", observedEvent.getFileContents());
			assertFalse(observedEvent.wasInitialReadIn());
		}
		else 
		{
			fail("test ReadingInFile has timed out");
		}
	}
	

	@Test
	public void testReadingInToolStreamAndParsing() 
	{
		
		fail("Not yet implemented");
	}
}
