package edu.ncsu.lubick;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.http.HTTPServer;

public class RunnerNoScreencasting
{

	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		LocalHub.startServer("HF/Screencasting/", Runner.DEFAULT_DB_LOC, true, false, false, false);
		Thread.sleep(1000);
		Desktop.getDesktop().browse(buildStartingURI());
	}

	private static URI buildStartingURI() throws URISyntaxException
	{
		//For testing sharing: 
		return new URI("http", null, "localhost", HTTPServer.SERVER_PORT, "/shareClip", "pluginName=Testing&toolName=Whombo #5&shareWithName=Test User&shareWithEmail=test@mailinator.com", null);
		//return new URI("http", null, "localhost", HTTPServer.SERVER_PORT, "/", null, null);
	}

}
