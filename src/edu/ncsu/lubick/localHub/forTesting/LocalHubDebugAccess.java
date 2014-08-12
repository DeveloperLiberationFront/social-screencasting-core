package edu.ncsu.lubick.localHub.forTesting;

import java.util.List;

import edu.ncsu.lubick.localHub.LocalHubProcess;
import edu.ncsu.lubick.localHub.ToolUsage;

public interface LocalHubDebugAccess extends LocalHubProcess {

	List<String> getAllPluginNames();

	void reportToolStream(List<ToolUsage> ts);

}
