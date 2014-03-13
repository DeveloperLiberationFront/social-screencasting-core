package edu.ncsu.lubick.localHub.database;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public abstract class LocalDBAbstraction
{

	public abstract List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	public abstract void storeToolUsage(ToolUsage tu, String associatedPlugin);

	public abstract void close();

	public abstract List<ToolUsage> getBestNInstancesOfToolUsage(int n, String pluginName, String toolName);

	public abstract List<String> getNamesOfAllPlugins();

	public abstract List<Integer> getTopScoresForToolUsage(int maxToolUsages, String pluginName, String toolName);

	public abstract List<String> getExcesiveTools();

	public abstract void deleteClipForToolUsage(String clipID);

	public abstract void createClipForToolUsage(String clipID, ToolUsage toolUsage);

	public abstract boolean isClipUploaded(String clipId);

	public abstract ToolUsage getToolUsageById(String clipId);

	public abstract void setClipUploaded(String clipId, boolean b);

}
