package edu.ncsu.lubick;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;

public class RunnerNoScreencasting
{

	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		LocalHub.startServer("HF/Screencasting/", Runner.DEFAULT_DB_LOC, true, false, true, false);
		Thread.sleep(1000);
		Desktop.getDesktop().browse(buildStartingURI());
	}

	private static URI buildStartingURI() throws URISyntaxException
	{
		//For testing sharing: URI u = new URI("http", null, "localhost", 4443, "/shareClip", "pluginName=Testing&toolName=Whombo #5&shareWithName=Test User&shareWithEmail=test@mailinator.com", null);
		URI u = new URI("http", null, "localhost", 4443, "/", null, null);
		return u;
	}

}
