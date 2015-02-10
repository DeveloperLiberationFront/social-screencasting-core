package edu.ncsu.lubick.localHub.database;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.lubick.localHub.http.HTTPUtils;

/**
 * Reports tools to the recommendation endpoint
 */
public class RecommendationToolReporter implements ExternalToolUsageReporter {
	
	public static final String RECOMMENDER_STAGING_TABLE_NAME = "RecommenderStage";
	
	private static URI endpointURL;
	
	private static final Logger logger;
	
	static {
		logger = Logger.getLogger(RecommendationToolReporter.class);
		try
		{
			endpointURL = HTTPUtils.buildExternalHttpURI("/events");
		}
		catch (URISyntaxException e)
		{
			logger.fatal("Cannot build endpoint");
			endpointURL = null;
		}
	}
	
	
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	
	private boolean isHttpUp = false;

	private UserManager userManager;

	public RecommendationToolReporter(UserManager userManager)
	{
		this.userManager =userManager;
	}

	@Override
	public boolean initialize()
	{
		//no special setup
		return endpointURL != null;
	}

	@Override
	public void setUpForReporting()
	{
		isHttpUp = true;
	}

	@Override
	public String getStagingName()
	{
		return RECOMMENDER_STAGING_TABLE_NAME;
	}

	@Override
	public boolean shouldSend()
	{
		return isHttpUp;
	}

	@Override
	public boolean reportTool(ToolUsage tu, String userid)
	{
		JSONObject objForSending = null;
		try
		{
			objForSending = makeSendingObj(tu);
		}
		catch (JSONException e)
		{
			logger.error("Could not report tool "+tu+" because JSON problems",e);
			return false;
		}
		logger.debug("Posting to "+endpointURL);
		HttpPost postRequest = new HttpPost(endpointURL);
		HTTPUtils.addAuth(postRequest, this.userManager);
		
		try {
			StringEntity input = new StringEntity(objForSending.toString());
			input.setContentType("application/json");
			postRequest.setEntity(input);
			 
			HttpResponse response = httpClient.execute(postRequest);
			 
			String responseString = HTTPUtils.getResponseBody(response);
			logger.trace(responseString);
			JSONObject responseObj = new JSONObject(responseString);
			if (!"OK".equals(responseObj.getString("_status"))) {
				logger.warn("There was a problem reporting toolusage event "+objForSending+" response: "+responseObj.toString(2));
				return false;
			}
			
			
			
		}
		catch (Exception e) {
			logger.warn("Unable to report event toolUsage object: "+objForSending, e);
			return false;
		}
		finally
		{
			postRequest.releaseConnection();
		}
		
		return true; 
	}

	private JSONObject makeSendingObj(ToolUsage tu) throws JSONException
	{
		JSONObject jobj = new JSONObject();
		jobj.put("application", tu.getApplicationName());
		jobj.put("tool", tu.getToolName());
		jobj.put("time", tu.getTimeStamp().getTime());
		jobj.put("bindingUsed", !ToolUsage.MENU_KEY_PRESS.equals(tu.getToolKeyPresses()));
		jobj.put("other", tu.getToolClass());
		
		return jobj;
	}

	@Override
	public void finishReporting()
	{
		//no cleanup
	}
	
	@SuppressWarnings("unused")
	private static void main(String[] args)
	{
		TestingUtils.makeSureLoggingIsSetUp();
		ToolUsage tu = new ToolUsage("fakeTool", "", "[GUI]", "[ScreencastingHub]", new Date(), 10, 10);
		
		UserManager um = new UnitTestUserManager(new File("."));
		RecommendationToolReporter rec = new RecommendationToolReporter(um);
		rec.reportTool(tu, "");
	}

}
