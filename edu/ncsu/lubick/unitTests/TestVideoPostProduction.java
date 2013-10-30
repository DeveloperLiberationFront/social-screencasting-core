package edu.ncsu.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

import edu.ncsu.lubick.localHub.FileUtilities;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.UtilitiesForTesting;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionVideoHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.VideoEncodingException;

public class TestVideoPostProduction
{
	private static final String TEST_PLUGIN_NAME = "Testing";
	private static final String DEFAULT_TESTING_KEYPRESS = "Ctrl+5";
	private static final String DEFAULT_TESTING_TOOL_CLASS = "Debug";

	private SimpleDateFormat dateInSecondsToNumber = FileUtilities.makeDateInSecondsToNumberFormatter();

	static
	{
		PropertyConfigurator.configure(LocalHub.LOGGING_FILE_PATH);
	}

	@Test
	public void testSingleToolUsageExtraction()
	{

		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		String toolName = "WhomboTool #1";

		assertTrue(capFile.exists());

		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());

		PostProductionVideoHandler handler = new PostProductionVideoHandler();
		handler.loadFile(capFile);

		handler.setCurrentFileStartTime(date);

		Date datePlusFifteen = new Date(date.getTime() + 15 * 1000); // plus
																		// fifteen
																		// seconds

		ToolUsage testToolUsage = makeToolUsage(datePlusFifteen, toolName);

		File outputFile = handler.extractVideoForToolUsage(testToolUsage);

		verifyVideoFileIsCorrectlyMade(outputFile);
		verifyVideoNamedProperly(outputFile, toolName);
	}

	@Test
	public void testSingleToolUsageExtractionReallyEarly()
	{

		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		String toolName = "WhomboTool #2";

		assertTrue(capFile.exists());

		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());

		PostProductionVideoHandler handler = new PostProductionVideoHandler();
		handler.loadFile(capFile);

		handler.setCurrentFileStartTime(date);

		Date datePlusOne = new Date(date.getTime() + 1 * 1000); // plus one
																// second

		ToolUsage testToolUsage = makeToolUsage(datePlusOne, toolName);

		File outputFile = handler.extractVideoForToolUsage(testToolUsage);

		verifyVideoFileIsCorrectlyMade(outputFile);
		verifyVideoNamedProperly(outputFile, toolName);
	}

	@Test
	public void testSingleToolUsageExtractionOverlappingFiles()
	{

		File firstcapFile = new File("./src/ForTesting/oneMinuteCap.cap");
		File secondCapFile = new File("./src/ForTesting/oneMinuteCap.cap"); // we'll just reuse this for testing
		String toolName = "WhomboTool #3";

		assertTrue(firstcapFile.exists());
		assertTrue(secondCapFile.exists());

		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());
		Date secondDate = UtilitiesForTesting.truncateTimeToMinute(new Date(date.getTime() + 61 * 1000));

		PostProductionVideoHandler handler = new PostProductionVideoHandler();
		handler.loadFile(firstcapFile);
		handler.enqueueOverLoadFile(secondCapFile, secondDate);

		handler.setCurrentFileStartTime(date);

		Date datePlusFiftyFive = new Date(date.getTime() + 55 * 1000); // plus 55 seconds, plenty to over run this file

		ToolUsage testToolUsage = makeToolUsage(datePlusFiftyFive, toolName);

		File outputFile = handler.extractVideoForToolUsage(testToolUsage);

		verifyVideoFileIsCorrectlyMade(outputFile);
		verifyVideoNamedProperly(outputFile, toolName);

	}

	@Test
	public void testBug8() throws Exception
	{ // https://bitbucket.org/klubick/screencasting-module/issue/8/extracting-some-tools-causes-a
		// This was an overlap problem.

		File firstcapFile = new File("./src/ForTesting/bug8.screencasts.28913211516.cap");
		File secondCapFile = new File("./src/ForTesting/bug8.screencasts.28913211615.cap");

		assertTrue(firstcapFile.exists());
		assertTrue(secondCapFile.exists());

		Date date = dateInSecondsToNumber.parse("28913211516");
		Date secondDate = dateInSecondsToNumber.parse("28913211615");

		assertNotNull(date);
		assertNotNull(secondDate);

		PostProductionVideoHandler handler = new PostProductionVideoHandler();
		handler.loadFile(firstcapFile);
		handler.enqueueOverLoadFile(secondCapFile, secondDate);

		handler.setCurrentFileStartTime(date);

		Date dateOfBuggyToolUsage = new Date(1381972574596L); // from the bug
																// report.

		IdealizedToolStream iToolStream = new IdealizedToolStream(date);

		iToolStream.addToolUsage("Open Call Hierarchy", "", "MENU", dateOfBuggyToolUsage, 15000);

		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin("Eclipse");
		assertEquals(1, toolStream.getAsList().size());

		File outputFile = null;
		try
		{
			outputFile = handler.extractVideoForToolUsageThrowingException(toolStream.getAsList().get(0));
			// throws an exception if there was a problem
		}
		catch (VideoEncodingException e)
		{
			e.printStackTrace();
			fail("Should not have caught an exception here");
		}

		verifyVideoFileIsCorrectlyMade(outputFile);

	}

	private ToolUsage makeToolUsage(Date toolUsageDate, String toolUsageName)
	{
		IdealizedToolStream iToolStream = new IdealizedToolStream(toolUsageDate);
		iToolStream.addToolUsage(toolUsageName, DEFAULT_TESTING_TOOL_CLASS, DEFAULT_TESTING_KEYPRESS, toolUsageDate, 15);

		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin(TEST_PLUGIN_NAME);
		assertEquals(1, toolStream.getAsList().size());

		ToolUsage testToolUsage = toolStream.getAsList().get(0);
		return testToolUsage;
	}

	private void verifyVideoFileIsCorrectlyMade(File outputFile)
	{
		assertNotNull(outputFile);
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		assertTrue(outputFile.getName().endsWith(".mkv"));
		assertTrue(outputFile.length() > 500000); // I expect the file size to
													// be at least 1 Mb and no
													// more than 2Mb
		assertTrue(outputFile.length() < 2000000);
	}

	private void verifyVideoNamedProperly(File outputFile, String toolName)
	{
		assertEquals(PostProductionVideoHandler.makeFileNameForToolPlugin(TEST_PLUGIN_NAME, toolName), outputFile.getPath());
	}

}
