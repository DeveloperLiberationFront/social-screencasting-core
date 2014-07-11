package edu.ncsu.lubick.localHub.database;

import java.util.List;

import edu.ncsu.lubick.localHub.ClipOptions;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.util.ToolCountStruct;

public abstract class LocalDBAbstraction
{

	public abstract List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName);

	public abstract void storeToolUsage(ToolUsage tu, String associatedPlugin);

	public abstract void close();

	public abstract List<ToolUsage> getBestNInstancesOfToolUsage(int n, String pluginName, String toolName, boolean isKeyboardShortcutHuh);

	public abstract List<String> getNamesOfAllPlugins();

	public abstract List<String> getExcesiveTools();

	public abstract void deleteClipForToolUsage(String clipID);

	public abstract boolean isClipUploaded(String clipId);

	public abstract ToolUsage getToolUsageById(String clipId);

	public abstract void setClipUploaded(String clipId, boolean b);

	public abstract Boolean setStartEndFrame(String folder, int startFrame, int endFrame);

	public abstract void createClipForToolUsage(String clipID, ToolUsage tu, ClipOptions clipOptions);

	public abstract ToolCountStruct getToolAggregate(String applicationName, String toolName);
}
