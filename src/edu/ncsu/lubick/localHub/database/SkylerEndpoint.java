package edu.ncsu.lubick.localHub.database;

import static edu.ncsu.lubick.localHub.database.EventForwarder.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.ToolUsage;

public class SkylerEndpoint implements ExternalToolUsageReporter {
	
	public static final String SKYLER_STAGING_TABLE_NAME = "SkylerStage";
	private static final Logger logger = Logger.getLogger(SkylerEndpoint.class);
	private Properties skylerProperties;
	
	private CloseableHttpClient httpClient;
	
	private boolean skylerAvailable = false;		//available 
	
	public SkylerEndpoint(Properties props) {
		this.skylerProperties = props;
		
		try {
			
			// httpClient = HttpClients.custom().build();
			httpClient = HttpClients.createSystem();
		 }
		 catch (Exception e) {
			 logger.warn("Unable to create custom certificatoin policy for Skylr connection");
			 httpClient =  HttpClientBuilder.create().build();
		 }
	}

	@Override
	public boolean initialize()
	{
		//no special setup here
		return true;
	}

	@Override
	public void setUpForReporting()
	{
		skylerAvailable = true;
	}

	@Override
	public String getStagingName()
	{
		return SKYLER_STAGING_TABLE_NAME;
	}

	@Override
	public boolean shouldSend()
	{
		return skylerAvailable;
	}

	@Override
	public boolean reportTool(ToolUsage tu, String userID)
	{
		try {
			JSONObject jsonTU = convertToolUsageToJSONObjectForSkylr(tu, userID);
			return this.insertToolUsageInSkylr(jsonTU, userID);


		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception ex) { // this is only thrown right now if the connection fails in isToolUsageInSkylr can't connect
			logger.warn("skylr unavailable (not able to connect) - skipping for this cycle"); 
			skylerAvailable = false;
		}
		return false;
	}
	
	/**
	 * Inserts a toolUsage JSON Object into skylr
	 * 
	 * @param httpClient
	 * @param joToolUsage
	 * @param userID
	 * @return
	 */
	private boolean insertToolUsageInSkylr(JSONObject joToolUsage, String userID) {
		boolean result = false;
		
		HttpPost postRequest = new HttpPost(skylerProperties.getProperty(PROPERTY_DEST_SKYLR_ADD_URL));
		postRequest.addHeader("authtoken",skylerProperties.getProperty(PROPERTY_DEST_SKYLR_AUTHENTICATION_TOKEN));
		try {

			StringEntity input = new StringEntity(joToolUsage.toString());
			input.setContentType("application/json");
			postRequest.setEntity(input);
			 
			HttpResponse response = httpClient.execute(postRequest);
			if (response.getStatusLine().getStatusCode() >= 400) {
				result = false;
				logger.warn("Skylr - unable in insert event - "+ response.getStatusLine().getStatusCode() +":  toolUsage object: "+joToolUsage);
				logger.warn(skylerProperties.getProperty(PROPERTY_DEST_SKYLR_ADD_URL));
				logger.warn(getResponseBodyAsString(response));
			}
			else {
				result = true;
				logger.trace("Skylr - inserted event - "+ response.getStatusLine().getStatusCode() +":  toolUsage object: "+joToolUsage);
			}
		}
		catch (Exception e) {
			logger.warn("Skylr - unable in insert event ("+ e.getMessage() +") - toolUsage object: "+joToolUsage);
			result = false;
			skylerAvailable = false;
		}
		finally
		{
			postRequest.releaseConnection();
		}
		
		
		return result; 
	}
	
	public static String getResponseBodyAsString(HttpResponse response) throws IOException, UnsupportedEncodingException
	{
		StringBuilder sb = new StringBuilder();
		InputStream ips  = response.getEntity().getContent();
		try(BufferedReader buf = new BufferedReader(new InputStreamReader(ips, StandardCharsets.UTF_8));)
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

	/**
	 * Creates a JSONObject (manually so that the property names can be 
	 * set appropriately for the Skylr destination 
	 * 
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject convertToolUsageToJSONObjectForSkylr(ToolUsage toolUsage, String userID) throws JSONException {
		JSONObject appData = new JSONObject();
		appData.put("rcdClassOfTool",toolUsage.getToolClass());
		appData.put("rcdClipScore", toolUsage.getUsageScore());
		appData.put("rcdOriginalID", toolUsage.getUseID());
		
		
		JSONObject contentObject = new JSONObject();
		contentObject.put("UserId", userID);
		contentObject.put("AppName", toolUsage.getApplicationName());
		contentObject.put("ProjId", "LAS/Recommender");
		contentObject.put("SysId", "Recommender");
		contentObject.put("ProjVer", "1.0");
		contentObject.put("EvtTime", toolUsage.getTimeStamp().getTime());
		contentObject.put("EvtEndTime", toolUsage.getTimeStamp().getTime() + toolUsage.getDuration());
		try {
			contentObject.put("NetAddr", java.net.Inet4Address.getLocalHost().getHostAddress());
		}
		catch (UnknownHostException uhe) {
			contentObject.put("NetAddr", "0.0.0.0");
		}
		contentObject.put("EvtType", "[GUI]".equals(toolUsage.getToolKeyPresses()) ? "mouse event: button click" :  "keyboardevent:key press");
		contentObject.put("EvtDesc", toolUsage.getToolKeyPresses());
        contentObject.put("EvtAction", toolUsage.getToolName());
		contentObject.put("AppData",appData);
		
		JSONObject result = new JSONObject();
		result.put("content", contentObject);
		
		return result;
	}
	
	/**
	 * Creates a JSONObject that represents a query to Skylr to see if a particular event
	 * has been uploaded or not 
	 * 
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject createFindQueryForUseID(ToolUsage toolUsage) throws JSONException {
		JSONObject query = new JSONObject();
		query.put("data.AppData.rcdOriginalID", toolUsage.getUseID());
		
		JSONObject result = new JSONObject();
		result.put("type", "find");
		result.put("query", query);
		
		return result;
	}

	@Override
	public void finishReporting()
	{
		//no special teardown
	}

}
