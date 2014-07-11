package edu.ncsu.lubick.localHub.database;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import edu.ncsu.lubick.util.NotImplementedException;

public class SerializablePreparedStatement extends AbstractPreparedStatement implements Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8928997169786152933L;
	
	private String queryStatement;
	private Queue<StoredParam> params = new LinkedList<>();

	public SerializablePreparedStatement(String statementQuery)
	{
		this.queryStatement = statementQuery;
	}

	@Override
	public void close() throws SQLException, NotImplementedException
	{
		//Because there are no DB resources tied up, nothing need be done
	}
	
	@Override
	public ResultSet executeQuery() throws SQLException, NotImplementedException
	{
		throw new NotImplementedException("Use executeQuery(Connection connection) instead");
	}
	
	@Override
	public int executeUpdate() throws SQLException, NotImplementedException
	{
		throw new NotImplementedException("Use executeUpdate(Connection connection) instead");
	}
	
	
	
	public ResultSet executeQuery(Connection connection)
	{
		try
		{
			PreparedStatement ps = connection.prepareStatement(queryStatement);
			updatePreparedStatementWithStoredParams(ps);
			return ps.executeQuery();
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("problem querying the stored statement", e);
		}
	}
	
	public void executeUpdate(Connection connection)
	{
		try(PreparedStatement ps = connection.prepareStatement(queryStatement);)
		{
			updatePreparedStatementWithStoredParams(ps);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("problem executing the stored statement", e);
		}
		
	}
	
	private void updatePreparedStatementWithStoredParams(PreparedStatement ps) throws SQLException
	{
		for(StoredParam sParam : params)
		{
			sParam.applyThisParamToPreparedStatement(ps);
		}
	}


	@Override
	public void setString(int parameterIndex, String x) throws SQLException
	{
		this.params.add(new StoredStringParam(parameterIndex, x));
	}
	
	@Override
	public void setInt(int parameterIndex, int x) throws SQLException
	{
		this.params.add(new StoredIntParam(parameterIndex, x));
	}
	
	@Override
	public void setLong(int parameterIndex, long x) throws SQLException
	{
		this.params.add(new StoredLongParam(parameterIndex, x));
	}

	
	
	abstract class StoredParam implements Serializable
	{
		private static final long serialVersionUID = 1L;
		protected int parameterIndex;
		protected Object paramaterObject;
		
		public StoredParam(int parameterIndex, Object o)
		{
			this.parameterIndex = parameterIndex;
			this.paramaterObject = o;
		}
		
		public abstract void applyThisParamToPreparedStatement(PreparedStatement ps) throws SQLException;
	}
	
	class StoredStringParam extends StoredParam
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -1159691991346496017L;

		public StoredStringParam(int parameterIndex, String string)
		{
			super(parameterIndex, string);
		}

		@Override
		public void applyThisParamToPreparedStatement(PreparedStatement ps) throws SQLException
		{
			ps.setString(parameterIndex, (String) paramaterObject);			
		}
	}
	
	class StoredIntParam extends StoredParam
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -1159691591346496017L;

		public StoredIntParam(int parameterIndex, int i)
		{
			super(parameterIndex, i);
		}

		@Override
		public void applyThisParamToPreparedStatement(PreparedStatement ps) throws SQLException
		{
			ps.setInt(parameterIndex, (int) paramaterObject);			
		}
	}

	class StoredLongParam extends StoredParam
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -1159691991246496017L;

		public StoredLongParam(int parameterIndex, long x)
		{
			super(parameterIndex, x);
		}

		@Override
		public void applyThisParamToPreparedStatement(PreparedStatement ps) throws SQLException
		{
			ps.setLong(parameterIndex, (long) paramaterObject);			
		}
	}
}