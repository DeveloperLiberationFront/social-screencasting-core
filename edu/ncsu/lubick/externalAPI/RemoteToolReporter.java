package edu.ncsu.lubick.externalAPI;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.NotificationManager;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.database.BufferedDatabaseManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.lubick.localHub.http.HTTPUtils;
import edu.ncsu.lubick.util.ToolCountStruct;

public class RemoteToolReporter {

	private static final Logger logger = Logger.getLogger(RemoteToolReporter.class);

	private static final long REPORTING_DELAY = 300_000;		//every 5 minutes report
	
	private BufferedDatabaseManager databaseManager;

	private UserManager userManager;

	private CloseableHttpClient client = HttpClients.createDefault();

	private Timer reportingTimer;

	private NotificationManager notificationManager;


	public RemoteToolReporter(BufferedDatabaseManager databaseManager, UserManager userManager, NotificationManager notificationManager)
	{
		this.databaseManager = databaseManager;
		this.userManager = userManager;
		this.notificationManager = notificationManager;
		
		
		beginReportingTools();
		
	}
	
	
	private void beginReportingTools()
	{
		logger.info("Starting the reporting of tools.  Expect the first to happen in 10 seconds");
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
		reportingTimer.schedule(reportingTask, 10_000, REPORTING_DELAY);
	}


	private void reportTools() throws JSONException
	{
		JSONObject reportingObject;
		try
		{
			reportingObject = assembleReportingJSONObject();
		}
		catch (NoToolDataException e)
		{
			logger.info("No tools to report.  Not reporting");
			return;
		}
		
		logger.debug("preparing to report data "+reportingObject.toString(2));

		HttpPut httpPut = new HttpPut(preparePutURL());

		try
		{
			StringEntity content = new StringEntity(reportingObject.toString());
			content.setContentType("application/json");

			httpPut.setEntity(content);
			try(CloseableHttpResponse response = client.execute(httpPut);)
			{
				String responseBody = HTTPUtils.getResponseBody(response);
				logger.info("response: " +responseBody);
				this.notificationManager.handlePossibleNotification(new JSONObject(responseBody));
			}

		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
		finally {
			httpPut.reset();
		}
		
	}


	private URI preparePutURL()
	{
		StringBuilder pathBuilder = new StringBuilder("/api/");
		pathBuilder.append(userManager.getUserEmail());
		try
		{
			return HTTPUtils.buildURI("http", HTTPUtils.BASE_URL, pathBuilder.toString(), userManager);
		}
		catch (URISyntaxException e)
		{
			logger.fatal("Could not encode URI",e);
			return null;
		}

	}


	private JSONObject assembleReportingJSONObject() throws JSONException, NoToolDataException
	{
		JSONObject pluginAggregate = makeAggregateForAllPlugins();
		
		JSONObject reportingObject = new JSONObject();
		reportingObject.put("data", pluginAggregate);
		return reportingObject;
	}

	
	private JSONObject makeAggregateForAllPlugins() throws NoToolDataException
	{
		List<String> plugins = databaseManager.getNamesOfAllNonHiddenPlugins(); 
		if (plugins.isEmpty()) 
		{
			throw new NoToolDataException();
		}
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
			JSONObject tempObject = new JSONObject();
			try
			{
				tempObject.put("gui", tcs.guiToolCount);
				tempObject.put("keyboard", tcs.keyboardCount);
				retVal.put(tcs.toolName, tempObject);
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
		this.reportingTimer = null;
	}
	
	
	private class NoToolDataException extends Exception {
		private static final long serialVersionUID = 1L;
		
	}
	
	
	@SuppressWarnings("unused")
	private static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		CloseableHttpClient client = HttpClients.createDefault();
		JSONObject reportingObject = new JSONObject("{\"data\": {\"Eclipse\": {\"Undo\": 10, \"Save\": 50, \"Toggle Comment\": 3}}}");
		
		logger.debug("preparing to report data "+reportingObject.toString(2));

		StringBuilder pathBuilder = new StringBuilder("/api/");
		
		UserManager um = new UnitTestUserManager("Kevins Bad Test", "kjlubick%2btest@ncsu.edu", "221ed3d8-6a09-4967-91b6-482783ec5313");
		
		pathBuilder.append(um.getUserEmail());
		URI u = HTTPUtils.buildURI("http", HTTPUtils.BASE_URL, pathBuilder.toString(), um);
		
		HttpPut httpPut = new HttpPut(u);
		
		logger.info(httpPut);

		try
		{
			StringEntity content = new StringEntity(reportingObject.toString());
			content.setContentType("application/json");

			httpPut.setEntity(content);
			try(CloseableHttpResponse response = client.execute(httpPut);)
			{
				String responseBody = HTTPUtils.getResponseBody(response);
				logger.info("response: " +responseBody);
			}

		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
	}

}
