package edu.ncsu.lubick.localHub;

import java.awt.PopupMenu;

public interface LocalHubProcess {
	
	boolean isRunning();
	void shutDown();
	void setTrayIconMenu(PopupMenu pm);

}
