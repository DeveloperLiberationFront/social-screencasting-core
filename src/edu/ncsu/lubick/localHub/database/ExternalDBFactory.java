package edu.ncsu.lubick.localHub.database;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;

public class ExternalDBFactory {

	public static final int MYSQL_IMPLEMENTATION = 0;
	public static final int DUMMY_IMPLEMENTATION = 1;
	
	private static final Logger logger = Logger.getLogger(ExternalDBFactory.class);

	
	
	private ExternalDBFactory(){}

	public static ExternalDBAbstraction createDatabase(UserManager um, int implementation)
	{
		if (implementation == MYSQL_IMPLEMENTATION)
		{
			return new RemoteMySQLDatabase(um);
		}
		else if (implementation == DUMMY_IMPLEMENTATION)
		{
			return new DummyRemoteDatabase();
		}
		logger.fatal("Unsupported Implementation "+implementation);
		return null;
	}

	static class DummyRemoteDatabase implements ExternalDBAbstraction {

		@Override
		public void storeToolUsage(ToolUsage tu, String associatedPlugin)
		{
			//This is a dummy implementation
		}

		@Override
		public void connect()
		{
			//This is a dummy implementation
			
		}
		
	}
	
}
