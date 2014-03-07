package edu.ncsu.lubick.localHub.database;

import java.io.File;
import java.util.Date;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.util.FileDateStructs;

public abstract class LocalDBAbstraction
{

	public abstract List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	public abstract void storeToolUsage(ToolUsage tu, String associatedPlugin);

	public abstract void close();

	public abstract List<ToolUsage> getBestNInstancesOfToolUsage(int n, String pluginName, String toolName);

	public abstract void storeVideoFile(File newVideoFile, Date videoStartTime, int durationOfClip);

	public abstract List<FileDateStructs> getVideoFilesLinkedToTimePeriod(Date timeStamp, int duration);

	public abstract List<String> getNamesOfAllPlugins();

	public abstract List<Integer> getTopScoresForToolUsage(int maxToolUsages, String pluginName, String toolName);


}
