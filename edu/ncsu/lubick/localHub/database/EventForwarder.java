package edu.ncsu.lubick.localHub.database;

import java.sql.*;

public class EventForwarder extends Thread {
	
	/** how long should the application sleep before checking for new events.  Specified in milliseconds */
	private long _sleepTime = 30*1000;
	
	private String _sourceDriver = "org.sqlite.JDBC";
	private String _sourceURL    = "jdbc:sqlite:C:/Users/John/recommender/toolstreams.sqlite";

	public void initializeDatabaseDrivers() throws ClassNotFoundException {
		 Class.forName(_sourceDriver);
	}
	
	public void run() {
    	
    	int i=0;
    	while (true) {
    		i++;
    		System.out.println(i);
    		
    	    try (java.sql.Connection conn = DriverManager.getConnection(_sourceURL)) {
    	    	Statement statement = conn.createStatement();
    	    	ResultSet rs = statement.executeQuery("select use_id, plugin_name, usage_timestamp, tool_name, tool_key_presses, class_of_tool, tool_use_duration, clip_score from ToolUsages");
    	    	while(rs.next()) {
    	    		// read the result set
    	    		System.out.println("Tool: " + rs.getString("plugin_name")+"-"+rs.getString("tool_name"));
    	      }
    	    }
    	    catch(SQLException e)
    	    {
    	      // if the error message is "out of memory", 
    	      // it probably means no database file is found
    	      System.err.println(e.getMessage());
    	    }
    		
    		
    		try {
    			Thread.sleep(_sleepTime);
    		}
    		catch (Exception e) {
    			System.err.println(e);
    		}
    	}
    }

    public static void main(String args[]) {
    	EventForwarder ef = new EventForwarder();
    	
    	try {
    		ef.initializeDatabaseDrivers();	
    		ef.start();
    		
    	}
    	catch (Exception e) {
    		System.err.println(e);
    	}
    	
        
    }
}
