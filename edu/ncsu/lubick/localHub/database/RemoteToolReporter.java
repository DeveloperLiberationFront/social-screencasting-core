package edu.ncsu.lubick.localHub.database;

import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.BufferedDatabaseManager;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.util.ToolCountStruct;

public class RemoteToolReporter {

	private static final Logger logger = Logger.getLogger(RemoteToolReporter.class);
	
	private BufferedDatabaseManager databaseManager;

	private UserManager userManager;
	


	public RemoteToolReporter(BufferedDatabaseManager databaseManager, UserManager userManager)
	{
		this.databaseManager = databaseManager;
		this.userManager = userManager;
	}
	
	
	private void reportTools() throws JSONException
	{
		
		JSONObject objectToReport = makeAggregateForAllPlugins();
		objectToReport.put("name", userManager.getUserName());
		objectToReport.put("email", userManager.getUserEmail());
		objectToReport.put("token", userManager.getUserToken());
	}
	
	private JSONObject makeAggregateForAllPlugins()
	{
		List<String> plugins = databaseManager.getNamesOfAllPlugins();
		JSONObject allPlugins = new JSONObject();
		
		for(String pluginName: plugins)
		{
			try
			{
				allPlugins.put(pluginName, this.getAggregateForPlugin(pluginName));
			}
			catch (JSONException e)
			{
				logger.error("Unusual JSON exception, squashing: ",e);
			}
		}
		
		JSONObject retVal = new JSONObject();
		try
		{
			retVal.put("data", allPlugins);
		}
		catch (JSONException e)
		{
			logger.error("Unusual JSON exception, squashing: ",e);
		}
		return retVal;
	}


	private JSONObject getAggregateForPlugin(String pluginName)
	{
		List<ToolCountStruct> counts = databaseManager.getAllToolAggregateForPlugin(pluginName);
		
		JSONObject retVal = new JSONObject();
		for(ToolCountStruct tcs: counts)
		{
			try
			{
				retVal.put(tcs.toolName, tcs.toolCount);
			}
			catch (JSONException e)
			{
				logger.error("Unusual JSON exception, squashing: ",e);
			}
		}
		return retVal;
	}

}
