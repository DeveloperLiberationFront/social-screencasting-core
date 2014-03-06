package edu.ncsu.lubick;

import java.awt.Desktop;
import java.net.URI;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;

public class RunnerNoScreencasting
{

	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		LocalHub.startServer("HF/Screencasting/", Runner.DEFAULT_DB_LOC, true, false, true, false);
		Thread.sleep(1000);
		Desktop.getDesktop().browse(new URI("http://localhost:4443/"));
	}

}
