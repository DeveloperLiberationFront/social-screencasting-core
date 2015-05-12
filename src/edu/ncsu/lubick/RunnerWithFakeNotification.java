package edu.ncsu.lubick;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.log4j.Logger;

import edu.ncsu.las.net.ssl.KeystoreProvider;
import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.LocalHubProcess;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.http.HTTPServer;

public class RunnerWithFakeNotification
{
	public static final String DEFAULT_DB_LOC = "toolstreams.sqlite";
	private static LocalHubProcess localHub;
	private static TrayIcon addedTrayIcon;

	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		KeystoreProvider.loadAsResource("edu/ncsu/las/net/ssl/client.ks", "edu/ncsu/las/net/ssl/client.ts", "changeit");

		setUpTrayIcon();
		localHub = LocalHub.startServer("HF/Screencasting/", Runner.DEFAULT_DB_LOC,
				true,  //want http
				false, //want screen recording
				false, //want remote tool reporting
				false); //is debug  (sets a dummy remote database, prevents screencasts from being made, etc)
		Thread.sleep(10000);
		getTrayIcon().displayMessage("Screencast Requested", "Bryant has requested a screencast for Rename - Refactor", MessageType.INFO);
	}
	
	private static URI buildStartingURI() throws URISyntaxException
	{
		return new URI("http", null, "localhost", HTTPServer.SERVER_PORT, "/", null, null);
	}
	
	private static URI buildShareScreencastURI() throws URISyntaxException
	{
		return new URI("http://localhost:4443/#/share/Eclipse/Rename%20-%20Refactor?share_with_name=Bryant&share_with_email=bryant@example.com&request_id=58372664&mock=true");
	}

	private static PopupMenu setUpTrayIcon()
	{
		if (!SystemTray.isSupported())
		{
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
				catch (IOException | URISyntaxException e)
				{
					Logger.getRootLogger().error("problem spawning desktop", e);
				}
			}
		});
		
		MenuItem request = new MenuItem("Respond To request for Rename - Refactor");
		request.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				try
				{
					Desktop.getDesktop().browse(buildShareScreencastURI());
				}
				catch (IOException | URISyntaxException e)
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

		pm.add(request);
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
			Logger.getRootLogger().error("Problem making tray icon", e);
		}
	}

	private static Image createImage(String path)
	{
		URL imageURL = RunnerWithFakeNotification.class.getResource(path);

		if (imageURL == null)
		{
			System.err.println("Resource not found: " + path);
			return null;
		}
		return Toolkit.getDefaultToolkit().getImage(imageURL);

	}

	public static TrayIcon getTrayIcon()
	{
		return addedTrayIcon;
	}

	public static void setTrayIcon(TrayIcon addedTrayIcon)
	{
		RunnerWithFakeNotification.addedTrayIcon = addedTrayIcon;
	}
}
