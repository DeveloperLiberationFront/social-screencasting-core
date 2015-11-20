package edu.ncsu.dlf.localHub.http;

import java.util.List;

import edu.ncsu.dlf.localHub.ToolUsage;

public interface WebToolReportingInterface {

	void updateActivity(String pluginName, boolean isActive);

	void reportToolStream(List<ToolUsage> ts);

}
