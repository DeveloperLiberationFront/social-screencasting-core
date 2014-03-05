package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Date;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.LocalHubDebugAccess;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class TestClipCreationLimits {

	
	
	
	private static final String TEST_SCREENCASTING_DIR = "test_screencasting/";
	private static final int MAX_TOOL_USAGES = 5;
	private LocalHubDebugAccess server;


	@BeforeClass
	public void setUpClass() throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		
		//TODO unzip if needed
		File dir = new File(TEST_SCREENCASTING_DIR);
		assertTrue(dir.isDirectory());
		assertTrue(dir.list().length > 10);
	}


	@Test
	public void testEndToEndCreationAndLimit() throws Exception
	{
		this.server = LocalHub.startTESTINGServerAndReturnDebugAccess(TEST_SCREENCASTING_DIR);
		assertNotNull(server);
		assertTrue(server.isRunning());
		
		IdealizedToolStream iToolStream = new IdealizedToolStream(new Date(61_000));
		for(int i = 0; i< MAX_TOOL_USAGES; i++)
		{
			iToolStream.addToolUsage("WhomboTool #5", "", "Ctrl+5", new Date(62_000 + 2000*i), 1500 + i);
		}
		
		ToolStream convertedToolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		convertedToolStream.setAssociatedPlugin(TestPostProductionHandler.TEST_PLUGIN_NAME);
	}

}
