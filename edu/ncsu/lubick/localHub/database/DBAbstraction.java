package edu.ncsu.lubick.localHub.database;

import java.io.File;
import java.util.Date;
import java.util.List;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public abstract class DBAbstraction 
{

	public abstract List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	public abstract void storeToolUsage(ToolUsage tu, String associatedPlugin);

	public abstract void close();

	public abstract ToolUsage getLastInstanceOfToolUsage(String pluginName, String toolName);
	
	public static class FileDateStructs {

		public File file;
		public Date startTime;
		public FileDateStructs(File file, Date startTime) {

			this.file = file;
			this.startTime = startTime;
		}

	}
}
