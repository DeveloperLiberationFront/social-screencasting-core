package edu.ncsu.lubick;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.net.URI;
import java.net.URL;

import javax.swing.ImageIcon;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.ncsu.lubick.localHub.LocalHub;

public class Runner
{
	static
	{
		try
		{
			URL url = Runner.class.getResource(LocalHub.LOGGING_FILE_PATH);
			PropertyConfigurator.configure(url);
			Logger.getRootLogger().info("Logging initialized");
		}
		catch (Exception e)
		{
			//load safe defaults
			BasicConfigurator.configure();
			Logger.getRootLogger().info("Could not load property file, loading defaults", e);
		}
	}
	public static void main(String[] args) throws Exception
	{
		setUpTrayIcon();
		LocalHub.startServerForUse("HF/", "kevinsDatabase.sqlite");
		Thread.sleep(1000);
		Desktop.getDesktop().browse(new URI("http://localhost:4443/"));
	}
	private static void setUpTrayIcon()
	{
		if (!SystemTray.isSupported()) {
			Logger.getRootLogger().info("SystemTray is not supported");
			return;
		}
		final TrayIcon trayIcon = new TrayIcon(createImage("images/bulb.gif", "tray icon"));
		final SystemTray tray = SystemTray.getSystemTray();

	}

	protected static Image createImage(String path, String description) {
		URL imageURL = Runner.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} 
		return (new ImageIcon(imageURL, description)).getImage();

	}
}
