package edu.ncsu.lubick.localHub;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

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
		List<ToolUsage> best = this.databaseManager.getBestNInstancesOfToolUsage(LocalHub.MAX_TOOL_USAGES, tu.getPluginName(), tu.getToolName());
		
		
		return best.contains(tu);
	}

}
