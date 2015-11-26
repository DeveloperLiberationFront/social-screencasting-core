package edu.ncsu.dlf.unitTests;

import static edu.ncsu.dlf.util.FileUtilities.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.ncsu.dlf.localHub.LocalHub;
import edu.ncsu.dlf.localHub.ToolUsage;
import edu.ncsu.dlf.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.dlf.localHub.forTesting.LocalHubDebugAccess;
import edu.ncsu.dlf.localHub.forTesting.TestingUtils;
import edu.ncsu.dlf.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.dlf.util.FileUtilities;

public class TestClipCreationLimits {

	private static final String TEST_SCREENCASTING_DIR = "test_screencasting/";
	private static final File renderedVideos = new File(PostProductionHandler.MEDIA_OUTPUT_FOLDER);
	private static final int MAX_TOOL_USAGES = LocalHub.MAX_TOOL_USAGES;

	private static final Logger logger = Logger.getLogger(TestClipCreationLimits.class);


	@BeforeClass
	public static void setUpClass() throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();

		//TODO unzip if needed
		File dir = new File(TEST_SCREENCASTING_DIR);
		assertTrue(dir.isDirectory());
		assertTrue(dir.list().length > 10);

		clearOutAllTestingMedia();
	}


	private static void clearOutAllTestingMedia()
	{
		String[] list = nonNull(renderedVideos.list());
		for(String packageName : list)
		{
			if (packageName.startsWith(TestPostProductionHandler.TEST_PLUGIN_NAME))
			{
				File expectedDir = new File(renderedVideos, packageName);
				logger.debug("clearing out and deleting "+expectedDir);
				if (expectedDir.exists())
				{
					assertTrue(TestingUtils.clearOutDirectory(expectedDir));
					assertTrue(expectedDir.delete());
				}
				else {
					fail(packageName+" really should exist... ["+expectedDir+"]");
				}
			}
		}
	}


	@Test
	public void testEndToEndCreationAndLimit() throws Exception
	{
		TestingUtils.clearOutTestDB();
		LocalHubDebugAccess server = LocalHub.startTESTINGServerAndReturnDebugAccess(TEST_SCREENCASTING_DIR);
		assertNotNull(server);
		assertTrue(server.isRunning());

		List<ToolUsage> toolStreamOfFourTools = makeToolStreamOfFour();
		List<ToolUsage>  toolStreamOfTwoTools = makeToolStreamOfMaxPlusOne();
		List<ToolUsage>  excellentExample = makeSuperiorExample();


		int presize = nonNull(renderedVideos.list()).length;

		server.reportToolStream(toolStreamOfFourTools);

		int midSize = nonNull(renderedVideos.list()).length;

		assertEquals(4, midSize - presize);
		//fail();

		server.reportToolStream(toolStreamOfTwoTools);

		int postSize = nonNull(renderedVideos.list()).length;

		assertEquals(MAX_TOOL_USAGES, postSize - presize);

		//First 5 instances will have been made, the sixth ignored
		List<String> verifiedFiles = checkFileNamesForFirstFive(toolStreamOfFourTools, toolStreamOfTwoTools);



		server.reportToolStream(excellentExample);

		String[] postList = nonNull(renderedVideos.list());
		postSize = postList.length;

		assertEquals(MAX_TOOL_USAGES, postSize - presize);

		//the last one (the shortest duration) should get swapped for 
		verifiedFiles.set(MAX_TOOL_USAGES-1, getFolderNameForToolUsage(excellentExample.get(0)));
	}


	private List<String> checkFileNamesForFirstFive(List<ToolUsage> toolStreamOfFourTools, List<ToolUsage> toolStreamOfTwoTools)
	{

		List<String> expectedToolUsageFiles = new ArrayList<>();

		for(ToolUsage tu : toolStreamOfFourTools)
		{
			String truncatedName = getFolderNameForToolUsage(tu);
			expectedToolUsageFiles.add(truncatedName);
		}

		for(int i = expectedToolUsageFiles.size();i<MAX_TOOL_USAGES;i++)
		{
			ToolUsage tu = toolStreamOfTwoTools.get(i-4);
			String truncatedName = getFolderNameForToolUsage(tu);
			expectedToolUsageFiles.add(truncatedName);
		}

		checkRenderedFilesForExistanceOfNames(expectedToolUsageFiles);

		return expectedToolUsageFiles;
	}


	private void checkRenderedFilesForExistanceOfNames(List<String> expectedNames)
	{
		int numHits = 0;
		String[] postList = nonNull(renderedVideos.list());
		for(String fileName: postList)
		{
			if (fileName.startsWith(TestPostProductionHandler.TEST_PLUGIN_NAME))
			{
				logger.debug("found created clip: "+fileName);
				assertTrue("Expecting to find "+fileName+" in "+expectedNames, expectedNames.contains(fileName));

				numHits++;
			}
		}

		assertEquals(MAX_TOOL_USAGES, numHits);
	}


	private String getFolderNameForToolUsage(ToolUsage tu)
	{
		return FileUtilities.makeLocalFolderNameForBrowserMediaPackage(tu , "kjlubick+test@ncsu.edu")
				.substring("renderedVideos/".length());
	}


	private List<ToolUsage> makeSuperiorExample()
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(new Date(61_000L));
		iToolStream.addToolUsage("WhomboTool #5", "", "Ctrl+5", new Date(64_000L + 2000*MAX_TOOL_USAGES), 2000);

		iToolStream.setAssociatedApplication(TestPostProductionHandler.TEST_PLUGIN_NAME);
		return iToolStream.getAsListOfActualToolUsages();
	}


	private List<ToolUsage> makeToolStreamOfMaxPlusOne()
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(new Date(61_000L));
		for(int i = 4; i< MAX_TOOL_USAGES+1; i++)
		{
			iToolStream.addToolUsage("WhomboTool #5", "", "Ctrl+5", new Date(62_000L + 2000*i), 1500 - i);
		}

		iToolStream.setAssociatedApplication(TestPostProductionHandler.TEST_PLUGIN_NAME);
		return iToolStream.getAsListOfActualToolUsages();
	}




	private List<ToolUsage>  makeToolStreamOfFour()
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(new Date(61_000L));
		for(int i = 0; i< 4; i++)
		{
			iToolStream.addToolUsage("WhomboTool #5", "", "Ctrl+5", new Date(62_000L + 2000*i), 1500 - i);
		}

		iToolStream.setAssociatedApplication(TestPostProductionHandler.TEST_PLUGIN_NAME);
		return iToolStream.getAsListOfActualToolUsages();
	}


}
