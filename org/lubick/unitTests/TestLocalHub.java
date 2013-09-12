package org.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lubick.localHub.LoadedFileEvent;
import org.lubick.localHub.LoadedFileListener;
import org.lubick.localHub.LocalHub;
import org.lubick.localHub.forTesting.LocalHubDebugAccess;
import org.lubick.localHub.forTesting.TestUtilities;

public class TestLocalHub {

	private static final String LOCAL_HUB_MONITOR_LOCATION = "HF/";
	private static LocalHubDebugAccess localHub;
	private static File testPluginDirectory;
	
	//testReadingInFile()
	private boolean readingInFileHasSeenResponse = false;
	private LoadedFileEvent readingInFileReturnedEvent = null;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
		localHub = LocalHub.startServerAndReturnDebugAccess(LOCAL_HUB_MONITOR_LOCATION);
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
	public void testReadingInFile() throws Exception
	{
		
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
		TestUtilities.createAbsoluteFileWithContent(testPluginDirectory.getAbsolutePath(),"TestPlugin.log","ThisIsAToolstream");
		
		fail("Not yet implemented");
		int timeCounter = 0;
		while (!readingInFileHasSeenResponse && timeCounter <30) 
		{
			Thread.sleep(1000);
			timeCounter++;
		}
		if (timeCounter <= 30 && readingInFileReturnedEvent != null)
		{
			assertEquals("TestPlugin.log", readingInFileReturnedEvent.getFileName());
			assertEquals("ThisIsAToolstream", readingInFileReturnedEvent.getFileContents());
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
