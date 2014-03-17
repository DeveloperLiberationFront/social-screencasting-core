package edu.ncsu.lubick.localHub;

import java.awt.Desktop;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.Runner;

public class NotificationManager {

	public void handlePossibleNotification(JSONObject responseObject)
	{
		if (!responseObject.has("notifications"))
		{
			return;
		}
		
		TrayIcon icon = Runner.getTrayIcon();
		if (icon != null) {
			String title = "Notification";
			try {
				JSONArray messages = responseObject.getJSONArray("notifications");
				JSONObject notification = messages.getJSONObject(0); 
				String message = notification.getString("message");
				final String pluginName = notification.getString("plugin");
				final String toolName = notification.getString("tool");
				JSONObject recipient = notification.getJSONObject("sender");
				final String recipientName = recipient.getString("name");
				final String recipientEmail = recipient.getString("email");
				
				icon.displayMessage(title, message, MessageType.INFO);
				icon.addActionListener(new ActionListener() {
	                public void actionPerformed(ActionEvent e) {
	                	//if the user clicks on the bubble, navigate to 'notifications'
	            		try {
	            			URI u = new URI("http", null, "localhost", 4443, "/shareClip",
	            						"pluginName="+pluginName +
	            						"&toolName="+toolName +
	            						"&shareWithName="+recipientName +
	            						"&shareWithEmail="+recipientEmail, null);
							Desktop.getDesktop().browse(u);
						} catch (IOException | URISyntaxException e1) {
							Logger.getRootLogger().error("Error loading notifications page", e1);
						}
	                }
	            });
			} catch (JSONException e) {
				Logger.getRootLogger().error("Error retrieving notification", e);
			}
		}
	}
}
