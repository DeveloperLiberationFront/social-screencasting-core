package edu.ncsu.lubick.externalAPI;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
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
		JSONArray reportingArray;
		try
		{
			reportingArray = makeAggregateForAllPlugins();
		}
		catch (NoToolDataException e)
		{
			logger.info("No tools to report.  Not reporting");
			return;
		}
		
		logger.debug("preparing to report data "+reportingArray);

		HttpPost httpPost = new HttpPost(preparePostURL());
		HTTPUtils.addAuth(httpPost, userManager);

		try
		{
			StringEntity content = new StringEntity(reportingArray.toString());
			content.setContentType("application/json");

			httpPost.setEntity(content);
			try(CloseableHttpResponse response = client.execute(httpPost);)
			{
				String responseBody = HTTPUtils.getResponseBody(response);
				logger.info("response: " +responseBody);
				if (responseBody.startsWith("<!DOCTYPE")) {
					logger.error("Got html message back, probably an error ");
					logger.debug(responseBody);
				} else {
					this.notificationManager.handlePossibleNotification(new JSONObject(responseBody));
				}
			}

		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
		finally {
			httpPost.releaseConnection();
		}
		
	}


	private URI preparePostURL()
	{
		StringBuilder pathBuilder = new StringBuilder("/api/");
		pathBuilder.append(userManager.getUserEmail());
		try
		{
			return HTTPUtils.buildExternalHttpURI("/report-usage");
		}
		catch (URISyntaxException e)
		{
			logger.fatal("Could not encode URI",e);
			return null;
		}

	}

	
	private JSONArray makeAggregateForAllPlugins() throws NoToolDataException
	{
		List<String> plugins = databaseManager.getNamesOfAllNonHiddenPlugins(); 
		if (plugins.isEmpty()) 
		{
			throw new NoToolDataException();
		}
		JSONArray allPlugins = new JSONArray();
		
		for(String pluginName: plugins)
		{
			allPlugins = this.getAggregateForApplication(pluginName, allPlugins);
		}
		
		return allPlugins;
	}


	private JSONArray getAggregateForApplication(String applicationName, JSONArray accumulator)
	{
		List<ToolCountStruct> counts = databaseManager.getAllToolAggregateForPlugin(applicationName);
		
		for(ToolCountStruct tcs: counts)
		{
			JSONObject tempObject = new JSONObject();
			try
			{
				tempObject.put("app_name", applicationName);
				tempObject.put("tool_name", tcs.toolName);
				tempObject.put("mouse", tcs.guiToolCount);
				tempObject.put("keyboard", tcs.keyboardCount);
				accumulator.put(tempObject);
			}
			catch (JSONException e)
			{
				logger.error("Unusual JSON exception, squashing: ",e);
			}
		}
		return accumulator;
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
		JSONArray reportingArray = new JSONArray("[{\"app_name\":\"Eclipse\", \"tool_name\": \"Toggle Comment\", \"keyboard\": 53, \"mouse\": 67}]");
		
		logger.debug("preparing to report data "+reportingArray.toString(2));

		UserManager um = new UnitTestUserManager("Kevins Bad Test", "kjlubick%2btest@ncsu.edu", "221ed3d8-6a09-4967-91b6-482783ec5313");
		
		URI u = HTTPUtils.buildExternalHttpURI("/report-usage");
		
		HttpPost httpPost = new HttpPost(u);
		HTTPUtils.addAuth(httpPost, um);
		
		
		logger.info(httpPost);

		try
		{
			StringEntity content = new StringEntity(reportingArray.toString());
			content.setContentType("application/json");

			httpPost.setEntity(content);
			try(CloseableHttpResponse response = client.execute(httpPost);)
			{
				String responseBody = HTTPUtils.getResponseBody(response);
				logger.info("response: " +responseBody);
			}

		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
		finally {
			httpPost.releaseConnection();
		}
	}

}
