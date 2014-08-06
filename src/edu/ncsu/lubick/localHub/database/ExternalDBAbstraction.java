package edu.ncsu.lubick.localHub.database;

import edu.ncsu.lubick.localHub.ToolUsage;

public interface ExternalDBAbstraction {

	void storeToolUsage(ToolUsage tu);

	void connect();

}
