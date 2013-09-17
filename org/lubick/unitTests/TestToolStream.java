package org.lubick.unitTests;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lubick.localHub.ToolStream;
import org.lubick.localHub.forTesting.IdealizedToolStream;

public class TestToolStream {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreationOfToolStream() {
		IdealizedToolStream iToolStream = new IdealizedToolStream();
		
		Date firstDate = new Date();
		Date secondDate = new Date(firstDate.getTime()+1000);
		
		iToolStream.addToolUsage("ToolString", "ClassString", "Keypresses",firstDate, 2);
		iToolStream.addToolUsage("WhomboTool #5", "Debug", "Ctrl+5",secondDate, 1);
		
		
		//I'm scoping this out to avoid copy+paste problems with these assertions
		{
			List<IdealizedToolStream.ToolUsage> tools = iToolStream.getAsList();
	
			assertEquals("ToolString", tools.get(0).getToolName());
			assertEquals("ClassString", tools.get(0).getToolClass());
			assertEquals("Keypresses", tools.get(0).getToolKeyPresses());
			assertEquals(firstDate, tools.get(0).getTimeStamp());
			assertEquals(2, tools.get(0).getDuration());
			
			assertEquals("WhomboTool #5", tools.get(1).getToolName());
			assertEquals("Debug", tools.get(1).getToolClass());
			assertEquals("Ctrl+5", tools.get(1).getToolKeyPresses());
			assertEquals(secondDate, tools.get(1).getTimeStamp());
			assertEquals(1, tools.get(1).getDuration());
		}
		
		ToolStream convertedToolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		
		assertTrue(iToolStream.isEquivalent(convertedToolStream));
		
		{
			List<ToolStream.ToolUsage> tools = convertedToolStream.getAsList();
		
			assertEquals("ToolString", tools.get(0).getToolName());
			assertEquals("ClassString", tools.get(0).getToolClass());
			assertEquals("Keypresses", tools.get(0).getToolKeyPresses());
			assertEquals(firstDate, tools.get(0).getTimeStamp());
			assertEquals(2, tools.get(0).getDuration());
			
			assertEquals("WhomboTool #5", tools.get(1).getToolName());
			assertEquals("Debug", tools.get(1).getToolClass());
			assertEquals("Ctrl+5", tools.get(1).getToolKeyPresses());
			assertEquals(secondDate, tools.get(1).getTimeStamp());
			assertEquals(1, tools.get(1).getDuration());
		}
		
	}

}
