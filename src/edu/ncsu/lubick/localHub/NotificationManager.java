package edu.ncsu.lubick.localHub;

import java.awt.Desktop;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.Runner;

public class NotificationManager {

	private PopupMenu popupMenu;
	
	private Set<MenuItem> notificationsPending = new HashSet<>();
	
	private MenuItem noNotifications = new MenuItem("No requests... for now");

	private NotificationListener listener;

	public NotificationManager(NotificationListener localHub)
	{
		this.listener = localHub;
	}

	public void handlePossibleNotification(JSONObject responseObject)
	{
		if (!responseObject.has("notifications"))
		{
			return;
		}
		
		
		
		TrayIcon icon = Runner.getTrayIcon();
		if (icon != null) {
			try {
				JSONArray messages = responseObject.getJSONArray("notifications");
				
				listener.notificationReceived(messages.toString(2));
				
				for(int i = 0;i< messages.length(); i++) {
					JSONObject notification = messages.getJSONObject(i); 
					if ("share_request".equals(notification.getString("type"))) {
						handleRequestNotification(icon, notification, messages.length() == 1);
					}
					else {
						handleShareNotification(icon,notification, messages.length() == 1);
					}
				}
				
				//show bulk message
				if (messages.length() > 1) {
					icon.displayMessage("Notifications", "You have recieved "+messages.length()+" notifications", MessageType.INFO);
				}
				
				
			} catch (JSONException e) {
				Logger.getRootLogger().error("Error retrieving notification", e);
			}
		}
	}

	private void handleShareNotification(TrayIcon icon, JSONObject notification, boolean showSpecificMessage) throws JSONException
	{
		String message = notification.getString("message");
		if (showSpecificMessage)
			icon.displayMessage("Notification", message, MessageType.INFO);
		
		
		final String pluginName = notification.getString("plugin");
		final String toolName = notification.getString("tool");
		JSONObject recipient = notification.getJSONObject("sender");
		final String sharerEmail = recipient.getString("email");
		
		final MenuItem newNotification = new MenuItem(message);
		newNotification.addActionListener(new ActionListener() {
		    @Override
			public void actionPerformed(ActionEvent e) {
		    	//if the user clicks on the bubble, navigate to 'notifications'
				try {
					URI u = makeShowShareURI(pluginName, sharerEmail);
					Desktop.getDesktop().browse(u);
					
					markNotificationAsHandled(newNotification);
					listener.notificationClickedOn("Tool shared notification by "+sharerEmail+" received ["+pluginName+":"+toolName+"]");
				} catch (IOException | URISyntaxException e1) {
					Logger.getRootLogger().error("Error loading notifications page", e1);
				}
		    }
		});
		
		addNotification(newNotification);
	}

	private void handleRequestNotification(TrayIcon icon, JSONObject notification, boolean showSpecificMessage) throws JSONException
	{
		String message = notification.getString("message");
		if (showSpecificMessage)
			icon.displayMessage("Notification", message, MessageType.INFO);
		
		final String pluginName = notification.getString("plugin");
		final String toolName = notification.getString("tool");
		JSONObject recipient = notification.getJSONObject("sender");
		final String recipientName = recipient.getString("name");
		final String recipientEmail = recipient.getString("email");
		
		final MenuItem newNotification = new MenuItem(message);
		newNotification.addActionListener(new ActionListener() {
		    @Override
			public void actionPerformed(ActionEvent e) {
		    	//if the user clicks on the bubble, navigate to 'notifications'
				try {
					URI u = makeShareRequestURI(pluginName, toolName, recipientName, recipientEmail);
					Desktop.getDesktop().browse(u);
					
					markNotificationAsHandled(newNotification);
					listener.notificationClickedOn(pluginName+":"+toolName+":"+recipientEmail);
				} catch (IOException | URISyntaxException e1) {
					Logger.getRootLogger().error("Error loading notifications page", e1);
				}
		    }
		});
		
		addNotification(newNotification);
	}

	private void addNotification(MenuItem newNotification)
	{
		if (notificationsPending.isEmpty() )
		{
			this.popupMenu.remove(noNotifications);
		}
		this.notificationsPending.add(newNotification);
		//inserts notification at the top of the list
		this.popupMenu.insert(newNotification, 0);
	}

	protected void markNotificationAsHandled(MenuItem oldNotification)
	{
		this.popupMenu.remove(oldNotification);
		this.notificationsPending.remove(oldNotification);
		if (this.notificationsPending.isEmpty()) 
		{
			this.popupMenu.insert(noNotifications, 0);
		}
	}

	public void setTrayIconMenu(PopupMenu pm)
	{
		this.popupMenu = pm;
		this.popupMenu.insertSeparator(0);
		this.popupMenu.insert(noNotifications, 0);
	}

	private URI makeShareRequestURI(String pluginName, String toolName, String recipientName,String recipientEmail) throws URISyntaxException
	{
		return new URI("http", null, "localhost", 4443, "/shareClip",
					"pluginName="+pluginName +
					"&toolName="+toolName +
					"&shareWithName="+recipientName +
					"&shareWithEmail="+recipientEmail, null);
	}

	private URI makeShowShareURI(String pluginName, String recipientEmail) throws URISyntaxException
	{
		return new URI("http", null, "localhost", 4443, "/",
				"pluginName="+pluginName +
				"&sharer="+recipientEmail, null);
	}
}