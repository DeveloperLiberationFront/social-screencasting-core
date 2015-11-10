package edu.ncsu.lubick.localHub.http;

import java.util.List;

import edu.ncsu.lubick.localHub.ToolUsage;

public interface WebToolReportingInterface {

	void updateActivity(String pluginName, boolean isActive);

	void reportToolStream(List<ToolUsage> ts);

}
