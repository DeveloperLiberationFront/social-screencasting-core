package edu.ncsu.dlf.localHub.forTesting;

import java.util.List;

import edu.ncsu.dlf.localHub.LocalHubProcess;
import edu.ncsu.dlf.localHub.ToolUsage;

public interface LocalHubDebugAccess extends LocalHubProcess {

	List<String> getAllPluginNames();

	void reportToolStream(List<ToolUsage> ts);

}
