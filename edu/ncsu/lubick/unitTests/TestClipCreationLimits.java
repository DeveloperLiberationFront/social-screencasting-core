package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.LocalHubDebugAccess;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.util.FileUtilities;

public class TestClipCreationLimits {

	private static final String TEST_SCREENCASTING_DIR = "test_screencasting/";
	private static final File renderedVideos = new File("renderedVideos/");
	private static final int MAX_TOOL_USAGES = 5;
	private LocalHubDebugAccess server;
	
	private static final Logger logger = Logger.getLogger(TestClipCreationLimits.class);


	@BeforeClass
	public static void setUpClass() throws Exception
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
		
		ToolStream toolStreamOfFourTools = makeToolStreamOfFour();
		ToolStream toolStreamOfTwoTools = makeToolStreamOfMaxPlusOne();
		
		clearOutAlreadyCreatedMedia(toolStreamOfFourTools);
		clearOutAlreadyCreatedMedia(toolStreamOfTwoTools);
		
		int presize = renderedVideos.list().length;
		
		server.reportToolStream(toolStreamOfFourTools);
		
		int midSize = renderedVideos.list().length;
		
		assertEquals(4, midSize - presize);
		
		server.reportToolStream(toolStreamOfTwoTools);
		
		String[] postList = renderedVideos.list();
		int postSize = postList.length;
		
		assertEquals(MAX_TOOL_USAGES, postSize - presize);
		
		//First 5 instances
		List<String> expectedToolUsageFiles = new ArrayList<>();
		
		for(ToolUsage tu : toolStreamOfFourTools.getAsList())
		{
			String truncatedName = FileUtilities.makeFolderNameForBrowserMediaPackage(tu, "kjlubick@ncsu.edu")
									.substring("renderedVideos/".length());
			expectedToolUsageFiles.add(truncatedName);
		}
		
		for(int i = expectedToolUsageFiles.size();i<MAX_TOOL_USAGES;i++)
		{
			ToolUsage tu = toolStreamOfTwoTools.getAsList().get(i-4);
			String truncatedName = FileUtilities.makeFolderNameForBrowserMediaPackage(tu , "kjlubick@ncsu.edu")
					.substring("renderedVideos/".length());
			expectedToolUsageFiles.add(truncatedName);
		}
		
		int numHits = 0;
		for(String fileName: postList)
		{
			if (fileName.startsWith("Testing"))
			{
				assertTrue(expectedToolUsageFiles.contains(fileName));
				numHits++;
			}
		}

		assertEquals(MAX_TOOL_USAGES, numHits);
	}


	private ToolStream makeToolStreamOfMaxPlusOne()
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(new Date(61_000L));
		for(int i = 4; i< MAX_TOOL_USAGES+1; i++)
		{
			iToolStream.addToolUsage("WhomboTool #5", "", "Ctrl+5", new Date(62_000L + 2000*i), 1500 - i);
		}
		
		ToolStream convertedToolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		convertedToolStream.setAssociatedPlugin(TestPostProductionHandler.TEST_PLUGIN_NAME);
		return convertedToolStream;
	}




	private ToolStream makeToolStreamOfFour()
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(new Date(61_000L));
		for(int i = 0; i< 4; i++)
		{
			iToolStream.addToolUsage("WhomboTool #5", "", "Ctrl+5", new Date(62_000L + 2000*i), 1500 - i);
		}
		
		ToolStream convertedToolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		convertedToolStream.setAssociatedPlugin(TestPostProductionHandler.TEST_PLUGIN_NAME);
		return convertedToolStream;
	}


	private void clearOutAlreadyCreatedMedia(ToolStream convertedToolStream)
	{
		for(ToolUsage tu :convertedToolStream.getAsList())
		{
			String name = FileUtilities.makeFolderNameForBrowserMediaPackage(tu, "kjlubick@ncsu.edu");
			File expectedDir = new File(name);
			if (expectedDir.exists())
			{
				assertTrue(TestingUtils.clearOutDirectory(expectedDir));
				assertTrue(expectedDir.delete());
			}
		}
	}

}
