package edu.ncsu.lubick.localHub.database;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public abstract class DBAbstraction 
{

	public abstract List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	public abstract void storeToolUsage(ToolUsage tu, String associatedPlugin);

	public abstract void close();
	
	
}
