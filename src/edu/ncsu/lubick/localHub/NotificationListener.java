package edu.ncsu.lubick.localHub;

public interface NotificationListener {

	void notificationReceived(String notifications);

	void notificationClickedOn(String notification);
}
