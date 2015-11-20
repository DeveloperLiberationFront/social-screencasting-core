package edu.ncsu.dlf;

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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.log4j.Logger;

import edu.ncsu.las.net.ssl.KeystoreProvider;
import edu.ncsu.dlf.localHub.LocalHub;
import edu.ncsu.dlf.localHub.LocalHubProcess;
import edu.ncsu.dlf.localHub.forTesting.TestingUtils;
import edu.ncsu.dlf.localHub.http.HTTPServer;

public class Runner
{
	public static final String DEFAULT_DB_LOC = "toolstreams.sqlite";
	private static LocalHubProcess localHub;
	private static TrayIcon addedTrayIcon;


	public static void main(String[] args) throws Exception
	{
		KeystoreProvider.loadAsResource("edu/ncsu/las/net/ssl/client.ks", "edu/ncsu/las/net/ssl/client.ts", "changeit");

		TestingUtils.makeSureLoggingIsSetUp();
		setUpTrayIcon();
		localHub = LocalHub.startServerForUse("HF/Screencasting/", DEFAULT_DB_LOC);
		Thread.sleep(1000);
		Desktop.getDesktop().browse(buildStartingURI());
	}
	
	private static URI buildStartingURI() throws URISyntaxException
	{
		return new URI("http", null, "localhost", HTTPServer.SERVER_PORT, "/", null, null);
	}


	private static PopupMenu setUpTrayIcon()
	{
		if (!SystemTray.isSupported()) {
			Logger.getRootLogger().info("SystemTray is not supported");
			return null;
		}
		final TrayIcon trayIcon = new TrayIcon(createImage("/imageAssets/tray_icon_small.png"));
		trayIcon.setImageAutoSize(true); 
		trayIcon.setToolTip("Social Screencasting running on port 4443");

		PopupMenu pm = new PopupMenu("Social Screencasting");
		MenuItem showUi = new MenuItem("Show UI");
		showUi.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				try
				{
					Desktop.getDesktop().browse(buildStartingURI());
				}
				catch (IOException|URISyntaxException e)
				{
					Logger.getRootLogger().error("problem spawning desktop", e);
				}
			}
		});
		
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				shutDown();
			}
		});
		
		pm.add(showUi);
		pm.insertSeparator(1);
		pm.add(exitItem);
		
		
		trayIcon.setPopupMenu(pm);


		addTrayIconToSystemTray(trayIcon);
		
		return pm;
	}

	public static void shutDown()
	{
		localHub.shutDown();
		cleanUpSystemTray();
	}

	private static void cleanUpSystemTray()
	{
		if (getTrayIcon() != null)
		{
			SystemTray.getSystemTray().remove(getTrayIcon());
		}
	}


	private static void addTrayIconToSystemTray(final TrayIcon trayIcon)
	{
		try
		{
			final SystemTray tray = SystemTray.getSystemTray();
			tray.add(trayIcon);
			setTrayIcon(trayIcon);
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
		return Toolkit.getDefaultToolkit().getImage(imageURL);

	}

	public static TrayIcon getTrayIcon() {
		return addedTrayIcon;
	}

	public static void setTrayIcon(TrayIcon addedTrayIcon) {
		Runner.addedTrayIcon = addedTrayIcon;
	}
}
