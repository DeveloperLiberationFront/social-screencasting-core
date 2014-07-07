package edu.ncsu.lubick.externalAPI;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.lubick.localHub.http.HTTPUtils;

public class ExternalShareClipRequester {

	
	private static final Logger logger = Logger.getLogger(ExternalShareClipRequester.class);
	private static CloseableHttpClient client = HttpClients.createDefault();
	private UserManager userManager;

	public ExternalShareClipRequester(UserManager userManager)
	{
		this.userManager = userManager;
	}
	

	public boolean requestClipsFromUser(String owner, String pluginName, String toolName)
	{
		try
		{
			requestClipsFromUserThrowingException(owner, pluginName, toolName);
			return true;
		}
		catch (URISyntaxException | JSONException e)
		{
			logger.error("Problem sharing clip", e);
			return false;
		}
	}


	private void requestClipsFromUserThrowingException(String owner, String pluginName, String toolName) throws URISyntaxException, JSONException
	{
		URI postUrl = preparePostURI();
		
		HttpPost httpPost = new HttpPost(postUrl);
		
		JSONObject dataWrapper = prepareDataWrapper(owner, pluginName, toolName);
		
		logger.debug("Requesting share: "+dataWrapper);
		try
		{
			StringEntity content = new StringEntity(dataWrapper.toString());
			content.setContentType("application/json");

			httpPost.setEntity(content);
			HttpResponse response = client.execute(httpPost);
			
			logger.info("Reply: "+HTTPUtils.getResponseBody(response));
		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
		finally {
			httpPost.reset();
		}
	}


	private JSONObject prepareDataWrapper(String owner, String pluginName, String toolName) throws JSONException
	{
		JSONObject dataWrapper = new JSONObject();
		JSONObject permissionsObject = new JSONObject();
		permissionsObject.put("plugin", pluginName);
		permissionsObject.put("creator", owner);
		permissionsObject.put("tool", toolName);

		dataWrapper.put("data", permissionsObject);
		return dataWrapper;
	}


	private URI preparePostURI() throws URISyntaxException
	{
		StringBuilder pathBuilder = new StringBuilder("/api/request-share");

		return HTTPUtils.buildExternalHttpURI(pathBuilder.toString(), userManager);
	}


	
	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		//UserManager userManager = new UnitTestUserManager("Test User", "test@mailinator.com", "123");
		UserManager userManager = new UnitTestUserManager("Kevin Test", "kjlubick+test@ncsu.edu", "221ed3d8-6a09-4967-91b6-482783ec5313");
		
		ExternalShareClipRequester sharer = new ExternalShareClipRequester(userManager);
		
		//sharer.requestClipsFromUser("kjlubick+test@ncsu.edu", "Eclipse", "Generate toString()");
		sharer.requestClipsFromUser("test@mailinator.com", "Eclipse", "Save");
		
	}

}
