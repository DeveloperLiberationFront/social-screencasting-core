package org.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Date;

import org.junit.Test;
import org.lubick.localHub.ToolStream;
import org.lubick.localHub.forTesting.IdealizedToolStream;
import org.lubick.localHub.forTesting.UtilitiesForTesting;
import org.lubick.localHub.videoPostProduction.PostProductionVideoHandler;

public class TestVideoPostProduction {

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
		
		assertTrue(outputFile.exists());
		assertTrue(outputFile.isFile());
		assertFalse(outputFile.isHidden());
		assertTrue(outputFile.length() > 1000);	//I expect the file size to be at least 1 Kb
		
		fail("Not yet implemented");
	}

}
