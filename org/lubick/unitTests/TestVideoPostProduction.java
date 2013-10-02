package org.lubick.unitTests;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class TestVideoPostProduction {

	@Test
	public void test() {
		
		File capFile = new File("./src/ForTesting/oneMinuteCap.cap");
		assertTrue(capFile.exists());
		
		fail("Not yet implemented");
	}

}
