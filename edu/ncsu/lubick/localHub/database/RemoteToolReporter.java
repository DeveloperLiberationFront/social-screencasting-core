package edu.ncsu.lubick.localHub.database;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.BufferedDatabaseManager;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.util.ToolCountStruct;

public class RemoteToolReporter {

	private static final Logger logger = Logger.getLogger(RemoteToolReporter.class);

	private static final long REPORTING_DELAY = 300_000;		//every 5 minutes report
	
	private BufferedDatabaseManager databaseManager;

	private UserManager userManager;

	private CloseableHttpClient client = HttpClients.createDefault();

	private Timer reportingTimer;


	public RemoteToolReporter(BufferedDatabaseManager databaseManager, UserManager userManager)
	{
		this.databaseManager = databaseManager;
		this.userManager = userManager;
		
		beginReportingTools();
	}
	
	
	private void beginReportingTools()
	{
		TimerTask reportingTask = new TimerTask() {
			
			@Override
			public void run()
			{
				try
				{
					reportTools();
				}
				catch (JSONException e)
				{
					logger.error("JSON Exception while reporting",e);
				}
			}
		};
		
		reportingTimer = new Timer(true);	//quit on application end
		reportingTimer.schedule(reportingTask, 10000, REPORTING_DELAY);
	}


	private void reportTools() throws JSONException
	{
		
		JSONObject reportingObject = assembleReportingJSONObject();
		
		logger.debug("preparing to report data "+reportingObject.toString(2));

		HttpPut httpPut = new HttpPut("http://screencaster-hub.appspot.com/api/"+userManager.getUserEmail());

		try
		{
			StringEntity content = new StringEntity(reportingObject.toString());
			content.setContentType("application/json");

			httpPut.setEntity(content);
			client.execute(httpPut);
		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
		
	}


	private JSONObject assembleReportingJSONObject() throws JSONException
	{
		JSONObject pluginAggregate = makeAggregateForAllPlugins();
		JSONObject userObject = assembleUserObject();
		JSONObject reportingObject = new JSONObject();
		reportingObject.put("user", userObject);
		reportingObject.put("data", pluginAggregate);
		return reportingObject;
	}


	private JSONObject assembleUserObject() throws JSONException
	{
		JSONObject userObject = new JSONObject();
		userObject.put("name", userManager.getUserName());
		userObject.put("email", userManager.getUserEmail());
		userObject.put("token", userManager.getUserToken());
		return userObject;
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
		
		return allPlugins;
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


	public void shutDown()
	{
		this.reportingTimer.cancel();
	}

}
