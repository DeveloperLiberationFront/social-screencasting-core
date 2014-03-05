package edu.ncsu.lubick.localHub.database;

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

import edu.ncsu.lubick.localHub.BufferedDatabaseManager;
import edu.ncsu.lubick.localHub.NotificationManager;
import edu.ncsu.lubick.localHub.UserManager;
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
		JSONObject reportingObject = assembleReportingJSONObject();
		
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
		
	}


	private URI preparePutURL()
	{
		StringBuilder pathBuilder = new StringBuilder("/api/");
		pathBuilder.append(userManager.getUserEmail());
		URI u;
		try
		{
			u = new URI("http", HTTPUtils.BASE_URL, pathBuilder.toString(), HTTPUtils.getUnEscapedUserAuthURL(userManager), null);
			return u;
		}
		catch (URISyntaxException e)
		{
			logger.fatal("Could not encode URI",e);
			return null;
		}

	}


	private JSONObject assembleReportingJSONObject() throws JSONException
	{
		JSONObject pluginAggregate = makeAggregateForAllPlugins();
		
		JSONObject reportingObject = new JSONObject();
		reportingObject.put("data", pluginAggregate);
		return reportingObject;
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
	
	
	@SuppressWarnings("unused")
	private static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		CloseableHttpClient client = HttpClients.createDefault();
		JSONObject reportingObject = new JSONObject("{\"data\": {\"Eclipse\": {\"Undo\": 10, \"Save\": 50, \"Toggle Comment\": 3}}}");
		
		logger.debug("preparing to report data "+reportingObject.toString(2));

		StringBuilder pathBuilder = new StringBuilder("/api/");
		
		UserManager um = new UnitTestUserManager("Test User", "test@mailinator.com", "123");
		
		pathBuilder.append(um.getUserEmail());
		URI u = new URI("http", HTTPUtils.BASE_URL, pathBuilder.toString(), HTTPUtils.getUnEscapedUserAuthURL(um), null);
		
		HttpPut httpPut = new HttpPut(u);

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
