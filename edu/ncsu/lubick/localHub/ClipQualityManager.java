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
		List<Integer> scores = this.databaseManager.getTopScoresForToolUsage(tu);
		return scores.size() == 0 || scores.get(scores.size()-1) < tu.getClipScore();
	}

}
