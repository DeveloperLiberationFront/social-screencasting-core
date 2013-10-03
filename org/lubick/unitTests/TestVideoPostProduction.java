package org.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Date;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;
import org.lubick.localHub.LocalHub;
import org.lubick.localHub.ToolStream;
import org.lubick.localHub.forTesting.IdealizedToolStream;
import org.lubick.localHub.forTesting.UtilitiesForTesting;
import org.lubick.localHub.videoPostProduction.PostProductionVideoHandler;

public class TestVideoPostProduction {
	
	static {
		PropertyConfigurator.configure(LocalHub.LOGGING_FILE_PATH);
	}

	@Test
	public void testSingleToolUsageExtraction() {
		
		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		
		
		assertTrue(capFile.exists());
		
		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());
		
		PostProductionVideoHandler handler = new PostProductionVideoHandler();
		handler.loadFile(capFile);
		
		handler.setCurrentFileStartTime(date);
		
		Date datePlusFifteen = new Date(date.getTime() + 15*1000);	//plus fifteen seconds
		
		IdealizedToolStream iToolStream = new IdealizedToolStream(date);
		iToolStream.addToolUsage("WhomboTool #5", "Debug", "Ctrl+5",datePlusFifteen, 1);
		
		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		assertEquals(1,toolStream.getAsList().size());
		
		File outputFile = handler.extractVideoForToolUsage(toolStream.getAsList().get(0));
		
		assertNotNull(outputFile);
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		assertTrue(outputFile.getName().endsWith(".mkv"));
		assertTrue(outputFile.length() > 1000000);	//I expect the file size to be at least 1 Mb and no more than 2Mb	
		assertTrue(outputFile.length() < 2000000);
	}
	
	@Test
	public void testSingleToolUsageExtractionReallyEarly() {
		
		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		
		
		assertTrue(capFile.exists());
		
		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());
		
		PostProductionVideoHandler handler = new PostProductionVideoHandler();
		handler.loadFile(capFile);
		
		handler.setCurrentFileStartTime(date);
		
		Date datePlusOne = new Date(date.getTime() + 1*1000);	//plus one second
		
		IdealizedToolStream iToolStream = new IdealizedToolStream(date);
		iToolStream.addToolUsage("WhomboTool #5", "Debug", "Ctrl+5",datePlusOne, 1);
		
		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		assertEquals(1,toolStream.getAsList().size());
		
		File outputFile = handler.extractVideoForToolUsage(toolStream.getAsList().get(0));
		
		assertNotNull(outputFile);
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		assertTrue(outputFile.getName().endsWith(".mkv"));
		assertTrue(outputFile.length() > 1000000);	//I expect the file size to be at least 1 Mb and no more than 2Mb	
		assertTrue(outputFile.length() < 2000000);
	}
	
	@Test
	public void testSingleToolUsageExtractionOverlappingFiles() {
		
		File firstcapFile = new File("./src/ForTesting/oneMinuteCap.cap");
		File secondCapFile = new File("./src/ForTesting/oneMinuteCap.cap");	//we'll just reuse this for testing
		
		assertTrue(firstcapFile.exists());
		assertTrue(secondCapFile.exists());
		
		Date date = UtilitiesForTesting.truncateTimeToMinute(new Date());
		
		PostProductionVideoHandler handler = new PostProductionVideoHandler();
		handler.loadFile(firstcapFile);
		handler.enqueueOverLoadFile(secondCapFile);
		
		handler.setCurrentFileStartTime(date);
		
		Date datePlusFiftyFive = new Date(date.getTime() + 55*1000);	//plus 55 seconds, plenty to over run this file
		
		IdealizedToolStream iToolStream = new IdealizedToolStream(date);
		iToolStream.addToolUsage("WhomboTool #5", "Debug", "Ctrl+5",datePlusFiftyFive, 1);
		
		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		assertEquals(1,toolStream.getAsList().size());
		
		File outputFile = handler.extractVideoForToolUsage(toolStream.getAsList().get(0));
		
		assertNotNull(outputFile);
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		assertTrue(outputFile.getName().endsWith(".mkv"));
		assertTrue(outputFile.length() > 1000000);	//I expect the file size to be at least 1 Mb and no more than 2Mb	
		assertTrue(outputFile.length() < 2000000);
		
		fail("Not implemented yet");
	}
	

}
