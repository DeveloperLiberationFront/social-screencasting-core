package edu.ncsu.lubick.localHub.database;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;

public class SerializablePreparedStatement extends AbstractPreparedStatement implements Serializable 
{

	public SerializablePreparedStatement(String statementQuery)
	{
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 8928997169786152933L;

	public ResultSet executeQuery(Connection connection)
	{
		// TODO Auto-generated method stub
		return null;
	}

	

}
