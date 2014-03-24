package edu.ncsu.lubick.localHub.database;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.UserManager;

public class ExternalDBFactory {

	public static final int MYSQL_IMPLEMENTATION = 0;
	
	private static final Logger logger = Logger.getLogger(ExternalDBFactory.class);
	
	private ExternalDBFactory(){}

	public static ExternalDBAbstraction createDatabase(UserManager um, int implementation)
	{
		if (implementation == MYSQL_IMPLEMENTATION)
		{
			return new RemoteMySQLDatabase(um);
		}
		logger.fatal("Unsupported Implementation "+implementation);
		return null;
	}

	
}
