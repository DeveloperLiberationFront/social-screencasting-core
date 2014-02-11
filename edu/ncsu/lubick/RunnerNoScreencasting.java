package edu.ncsu.lubick;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.ncsu.lubick.localHub.LocalHub;

public class RunnerNoScreencasting
{

	static
	{
		try
		{
			URL url = Runner.class.getResource("/etc/log4j.settings");
			PropertyConfigurator.configure(url);
			Logger.getRootLogger().info("Logging initialized");
		}
		catch (Exception e)
		{
			// load safe defaults
			BasicConfigurator.configure();
			Logger.getRootLogger().info("Could not load property file, loading defaults", e);
		}
	}

	public static void main(String[] args) throws Exception
	{
		LocalHub.startServer("HF/", "kevinsDatabase.sqlite", true, false, false);
		Thread.sleep(1000);
		Desktop.getDesktop().browse(new URI("http://localhost:4443/"));
	}

}
