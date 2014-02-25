package edu.ncsu.lubick.localHub.database;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
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

	private CloseableHttpClient client = HttpClients.createDefault();


	public RemoteToolReporter(BufferedDatabaseManager databaseManager, UserManager userManager)
	{
		this.databaseManager = databaseManager;
		this.userManager = userManager;
	}
	
	
	public void reportTools() throws JSONException
	{
		
		JSONObject objectToReport = makeAggregateForAllPlugins();
		JSONObject userObject = new JSONObject();
		userObject.put("name", userManager.getUserName());
		userObject.put("email", userManager.getUserEmail());
		userObject.put("token", userManager.getUserToken());

		
		HttpPut httpPut = new HttpPut("http://screencaster-hub.appspot.com/api/"+userManager.getUserEmail()+"/");

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("user", userObject.toString()));
		nvps.add(new BasicNameValuePair("data", objectToReport.toString()));
		
		logger.debug("reporting data "+objectToReport.toString(2));

		try
		{
			httpPut.setEntity(new UrlEncodedFormEntity(nvps));
			client.execute(httpPut);
		}
		catch (IOException e)
		{
			logger.fatal("Problem reporting tool info",e);
		}
		
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

}
