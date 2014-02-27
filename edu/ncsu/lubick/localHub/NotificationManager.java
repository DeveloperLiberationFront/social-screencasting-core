package edu.ncsu.lubick.localHub;

import org.json.JSONObject;

public class NotificationManager {

	public void handlePossibleNotification(JSONObject responseObject)
	{
		if (!responseObject.has("notification"))
		{
			return;
		}
		//TODO handle notification
		
	}

}
