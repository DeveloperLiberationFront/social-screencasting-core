package org.lubick.localHub.database;

import java.util.List;

import org.lubick.localHub.ToolStream.ToolUsage;

public abstract class DBAbstraction 
{

	public abstract List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);
	
	
}
