package edu.ncsu.lubick.localHub.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;

public abstract class RemoteSQLDatabase implements RemoteDBAbstraction {


	protected abstract PreparedStatement makePreparedStatement(String statementQuery);
	protected abstract void executeStatementWithNoResults(PreparedStatement statement);
	protected abstract ResultSet executeWithResults(PreparedStatement statement);
	protected abstract Logger getLogger();
	
	private String userId;
	
	public RemoteSQLDatabase(String userId)
	{
		if (userId == null)
		{
			throw new RuntimeException("User ID Cannot be null!");
		}
		this.userId = userId;
	}
	
	@Override
	public void registerNewUser(String newUserEmail, String newUserName, String newUserId)
	{	
		getLogger().debug("Making new user "+newUserEmail+", "+newUserName+", "+newUserId);
		String sql = "INSERT INTO user_table (user_id, email, name) VALUES(?,?,?)";
		
		try (PreparedStatement statement = makePreparedStatement(sql);)
		{
			statement.setString(1, newUserId);
			statement.setString(2, newUserEmail);
			statement.setString(3, newUserName);
			
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem in registerNewUser()", e);
		}
	}
	
	@Override
	public void storeToolUsage(ToolUsage tu, String associatedPlugin)
	{
		getLogger().debug("Storing tool usage to Remote Database "+tu);
		
		String sql ="INSERT INTO tool_info_by_user (user_id, plugin_name, " 
					+"tool_name, usage_timestamp, tool_keypress, class_of_tool, "
					+"tool_use_duration ) VALUES (?,?,?,?,?,?,?)";

		
		try (PreparedStatement statement = makePreparedStatement(sql);)
		{
			statement.setString(1, this.userId);
			statement.setString(2, associatedPlugin);
			statement.setString(3, tu.getToolName());
			statement.setLong(4, tu.getTimeStamp().getTime());
			statement.setString(5, tu.getToolKeyPresses());
			statement.setString(6, tu.getToolClass());
			statement.setInt(7, tu.getDuration());
			executeStatementWithNoResults(statement);
		}
		catch (SQLException e)
		{
			throw new DBAbstractionException("There was a problem in storeToolUsage()", e);
		}
	
	}
	
	
	public String getUserId()
	{
		return userId;
	}
	
}
