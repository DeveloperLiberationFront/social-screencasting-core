package edu.ncsu.lubick;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.LocalHubProcess;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;

public class Runner
{
	private static LocalHubProcess localHub;
	private static TrayIcon addedTrayIcon;

	static
	{
		TestingUtils.makeSureLoggingIsSetUp();
	}
	public static void main(String[] args) throws Exception
	{
		setUpTrayIcon();
		localHub = LocalHub.startServerForUse("HF/", "kevinsDatabase.sqlite");
		Thread.sleep(1000);
		Desktop.getDesktop().browse(new URI("http://localhost:4443/"));
	}


	private static void setUpTrayIcon()
	{
		if (!SystemTray.isSupported()) {
			Logger.getRootLogger().info("SystemTray is not supported");
			return;
		}
		final TrayIcon trayIcon = new TrayIcon(createImage("/imageAssets/tray_icon_small.png"));
		trayIcon.setImageAutoSize(true); 
		trayIcon.setToolTip("Social Screencasting running on port 4443");

		PopupMenu pm = new PopupMenu("Social Screencasting");
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				shutDown();
			}
		});
		pm.add(exitItem);
		trayIcon.setPopupMenu(pm);


		addTrayIconToSystemTray(trayIcon);
	}

	public static void shutDown()
	{
		localHub.shutDown();
		cleanUpSystemTray();
	}


	private static void cleanUpSystemTray()
	{
		if (addedTrayIcon != null)
		{
			SystemTray.getSystemTray().remove(addedTrayIcon);
		}
	}


	private static void addTrayIconToSystemTray(final TrayIcon trayIcon)
	{
		try
		{
			final SystemTray tray = SystemTray.getSystemTray();
			tray.add(trayIcon);
			addedTrayIcon = trayIcon;
		}
		catch (AWTException e)
		{
			Logger.getRootLogger().error("Problem making tray icon",e);
		}
	}

	private static Image createImage(String path) {
		URL imageURL = Runner.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} 
		Image img = Toolkit.getDefaultToolkit().getImage(imageURL);

		return img;

	}
}
