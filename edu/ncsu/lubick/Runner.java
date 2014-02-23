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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
		final TrayIcon trayIcon = new TrayIcon(createImage("/imageAssets/tray_icon_small.png"));
		trayIcon.setImageAutoSize(true);
		trayIcon.setToolTip("Social Screencasting running on port 4443");
		trayIcon.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					PopupMenu pm = new PopupMenu("Social Screencasting");
					MenuItem exitItem = new MenuItem("Exit");
					exitItem.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent arg0)
						{
							System.exit(0);
						}
					});
				}
			}
			
		});
		
		
		addTrayIconToSystemTray(trayIcon);
	}


	private static void addTrayIconToSystemTray(final TrayIcon trayIcon)
	{
		try
		{
			final SystemTray tray = SystemTray.getSystemTray();
			tray.add(trayIcon);
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
