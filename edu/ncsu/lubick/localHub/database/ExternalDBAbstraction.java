package edu.ncsu.lubick.localHub.database;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public interface ExternalDBAbstraction {

	void storeToolUsage(ToolUsage tu, String associatedPlugin);

}
