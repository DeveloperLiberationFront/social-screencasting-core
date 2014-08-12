package edu.ncsu.lubick.localHub.database;

import static edu.ncsu.lubick.localHub.database.EventForwarder.*;

import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.ToolUsage;
import edu.ncsu.lubick.localHub.http.HTTPUtils;

public class SkylerEndpoint implements ExternalToolUsageReporter {
	
	public static final String SKYLER_STAGING_TABLE_NAME = "SkylerStage";
	private static final Logger logger = Logger.getLogger(SkylerEndpoint.class);
	private Properties skylerProperties;
	
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	
	private boolean skylerAvailable = false;		//available 
	
	public SkylerEndpoint(Properties props) {
		this.skylerProperties = props;
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
			if (!isToolUsageInSkylr(tu)) { 
				JSONObject jsonTU = convertToolUsageToJSONObjectForSkylr(tu, userID);
				return this.insertToolUsageInSkylr(jsonTU, userID);
			}
			return true;
	
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
	 * Checks if the current tool usage is in the remote destination or not.
	 * Since we don't delete or track destinations sent success, we need to prevent 
	 * duplicates from being sent. 
	 * @param toolUsage
	 * @return
	 */
	private boolean isToolUsageInSkylr(ToolUsage toolUsage) throws Exception {
		boolean result = false;
		
		HttpPost postRequest = new HttpPost(skylerProperties.getProperty(PROPERTY_DEST_SKYLR_QUERY_URL));
		try {
			StringEntity input = new StringEntity(createFindQueryForUseID(toolUsage).toString());
			input.setContentType("application/json");
			postRequest.setEntity(input);
			 
			HttpResponse response = httpClient.execute(postRequest);
			 
			if (response.getStatusLine().getStatusCode() >= 400) {
				result = false;
				logger.warn("Skylr - unable to find existing object - "+ response.getStatusLine().getStatusCode() +":  toolUsage use ID: "+toolUsage.getUseID());
			}
			else {
				String responseBody = HTTPUtils.getResponseBody(response);
				
				JSONObject responseObject = new JSONObject();
				responseObject.put("results", new JSONObject(responseBody));
				
				if (responseObject.getJSONArray("results").length() >0) {
					result = true;
				}
			}
		}
		catch (Exception e) {
			if (e.getMessage().contains("Connection refused")) {
				throw new DBAbstractionException("skylr down", e);
			}
			logger.warn("Skylr - unable in find existing object - toolUsage use ID: "+toolUsage.getUseID(), e);
			result = false;
		}
		finally{
			postRequest.releaseConnection();
		}
		
		
		return result; 
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
		try {
			StringEntity input = new StringEntity(joToolUsage.toString());
			input.setContentType("application/json");
			postRequest.setEntity(input);
			 
			HttpResponse response = httpClient.execute(postRequest);
			 
			if (response.getStatusLine().getStatusCode() >= 400) {
				result = false;
				logger.warn("Skylr - unable in insert event - "+ response.getStatusLine().getStatusCode() +":  toolUsage object: "+joToolUsage);
			}
			else {
				result = true;
				logger.trace("Skylr - inserted event - "+ response.getStatusLine().getStatusCode() +":  toolUsage object: "+joToolUsage);
			}
		}
		catch (Exception e) {
			logger.warn("Skylr - unable in insert event ("+ e.getMessage() +") - toolUsage object: "+joToolUsage);
			result = false;
		}
		postRequest.releaseConnection();
		
		return result; 
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
