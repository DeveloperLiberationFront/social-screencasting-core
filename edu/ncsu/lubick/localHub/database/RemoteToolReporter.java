package edu.ncsu.lubick.localHub.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.BufferedDatabaseManager;
import edu.ncsu.lubick.localHub.NotificationManager;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.util.ToolCountStruct;

public class RemoteToolReporter {

	private static final String BASE_URL = "http://screencaster-hub.appspot.com/api/";

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
		reportingTimer.schedule(reportingTask, 0*10000, REPORTING_DELAY);
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
				String responseBody = getResponseBody(response);
				logger.info("response: " +responseBody);
				this.notificationManager.handlePossibleNotification(new JSONObject(responseBody));
			}

		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
		
	}


	private String getResponseBody(CloseableHttpResponse response) throws IOException, UnsupportedEncodingException
	{
		StringBuilder sb = new StringBuilder();
		InputStream ips  = response.getEntity().getContent();
		try(BufferedReader buf = new BufferedReader(new InputStreamReader(ips,"UTF-8"));)
		{
			
		    String s;
			while(true )
		    {
		        s = buf.readLine();
		        if(s==null || s.length()==0)
		            break;
		        sb.append(s);

		    }
		
			
		}
		return sb.toString();
	}


	private String preparePutURL()
	{
		return BASE_URL+userManager.getUserEmail()+"?"+makeAuthParameters();
	}


	private String makeAuthParameters()
	{
		return URLEncodedUtils.format(assembleUserObject(), "UTF-8");
	}


	private JSONObject assembleReportingJSONObject() throws JSONException
	{
		JSONObject pluginAggregate = makeAggregateForAllPlugins();
		
		JSONObject reportingObject = new JSONObject();
		reportingObject.put("data", pluginAggregate);
		return reportingObject;
	}


	
	private List<NameValuePair> assembleUserObject()
	{
		List<NameValuePair> retVal = new ArrayList<>();
		retVal.add(new BasicNameValuePair("name", userManager.getUserName()));
		retVal.add(new BasicNameValuePair("email", userManager.getUserEmail()));
		retVal.add(new BasicNameValuePair("token", userManager.getUserToken()));
		return retVal;
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
