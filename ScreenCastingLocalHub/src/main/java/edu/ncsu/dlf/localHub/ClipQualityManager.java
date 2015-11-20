package edu.ncsu.dlf.localHub;

import java.util.List;

import edu.ncsu.dlf.localHub.database.BufferedDatabaseManager;

public class ClipQualityManager {

	private BufferedDatabaseManager databaseManager;

	public ClipQualityManager(BufferedDatabaseManager databaseManager)
	{
		this.databaseManager = databaseManager;
	}

	public boolean shouldMakeClipForUsage(ToolUsage tu)
	{
		//TODO needs to be a bit more complicated.  The tool will already be reported, so we have to see if it's in the
		//top Max
		boolean isGUI = ToolUsage.MENU_KEY_PRESS.equals(tu.getToolKeyPresses());
		List<ToolUsage> best = this.databaseManager.getBestNInstancesOfToolUsage(LocalHub.MAX_TOOL_USAGES, tu.getApplicationName(), tu.getToolName(), !isGUI);
		
		
		return best.contains(tu);
	}

}
