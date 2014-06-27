package edu.ncsu.lubick.localHub.http;

import edu.ncsu.lubick.localHub.ToolStream;

public interface WebToolReportingInterface {

	void reportToolStream(ToolStream ts);

	void updateActivity(String pluginName, boolean isActive);

}
