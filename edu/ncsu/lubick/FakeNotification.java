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

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;

public class FakeNotification {

	
	private static TrayIcon trayIcon;

	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		PopupMenu pm = setUpTrayIcon();
		
		LocalHub.startServerForUse("HF/Screencasting/", "C:\\Users\\KevinLubick\\Desktop\\Sandbox\\toolstreams.sqlite");
		Thread.sleep(7_000);	
		
		String message = "Samuel Christie has requested to see Open Call Hierarchy";
		
		trayIcon.displayMessage("Notification", message, MessageType.INFO);
		final MenuItem newNotification = new MenuItem(message);
		newNotification.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
            	//if the user clicks on the bubble, navigate to 'notifications'
        		try {
        			URI u = new URI("http", null, "localhost", 4443, "/shareClip",
        						"pluginName="+"Eclipse" +
        						"&toolName="+"Open Call Hierarchy" +
        						"&shareWithName="+"Samuel Christie" +
        						"&shareWithEmail="+"schrist@ncsu.edu", null);
					Desktop.getDesktop().browse(u);
					
				} catch (IOException | URISyntaxException e1) {
					Logger.getRootLogger().error("Error loading notifications page", e1);
				}
            }
        });
		
		pm.insert(newNotification, 0);
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
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				System.exit(0);
			}
		});
		pm.add(exitItem);
		trayIcon.setPopupMenu(pm);


		addTrayIconToSystemTray(trayIcon);
		
		return pm;
	}
	
	private static void addTrayIconToSystemTray(final TrayIcon trayIcon)
	{
		try
		{
			final SystemTray tray = SystemTray.getSystemTray();
			tray.add(trayIcon);
			FakeNotification.trayIcon = trayIcon;
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
}

