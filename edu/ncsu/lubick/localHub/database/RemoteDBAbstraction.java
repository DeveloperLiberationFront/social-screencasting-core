package edu.ncsu.lubick.localHub.database;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;


public interface RemoteDBAbstraction {

	void close();

	void storeToolUsage(ToolUsage tu, String associatedPlugin);

	String registerNewUser(String newUserEmail, String newUserName);

}
