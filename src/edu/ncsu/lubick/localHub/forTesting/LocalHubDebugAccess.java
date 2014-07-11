package edu.ncsu.lubick.localHub.forTesting;

import java.util.List;

import edu.ncsu.lubick.localHub.LocalHubProcess;
import edu.ncsu.lubick.localHub.ToolStream;

public interface LocalHubDebugAccess extends LocalHubProcess {

	List<String> getAllPluginNames();

	void reportToolStream(ToolStream ts);

}
