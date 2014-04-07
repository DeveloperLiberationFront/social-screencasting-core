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

public class BrowserMediaPackageSharer {
	
	
	private static final Logger logger = Logger.getLogger(BrowserMediaPackageSharer.class);
	private static CloseableHttpClient client = HttpClients.createDefault();
	private UserManager userManager;

	public BrowserMediaPackageSharer(UserManager userManager)
	{
		this.userManager = userManager;
	}

	@SuppressWarnings("unused")
	private static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		UserManager userManager = new UnitTestUserManager("Kevin Lubick", "kjlubick@ncsu.edu", "0216427e-b0e0-4102-8274-9ce3bc7a74a0");
		
		
		BrowserMediaPackageSharer sharer = new BrowserMediaPackageSharer(userManager);
		
		sharer.shareClipWithUser("Eclipse857d7529-773c-3459-abbc-6f16f7b98418", "schrist@ncsu.edu");
		
	}
	
	public boolean shareClipWithUser(String clipId, String email)
	{
		try
		{
			shareClipWithUserThrowingException(clipId, email);
			return true;
		}
		catch (URISyntaxException | JSONException e)
		{
			logger.error("Problem sharing clip", e);
			return false;
		}
		
	}

	private void shareClipWithUserThrowingException(String clipId, String email) throws URISyntaxException, JSONException
	{
		URI postUrl = preparePostURI();
		
		HttpPost httpPost = new HttpPost(postUrl);
		
		JSONObject dataWrapper = prepareDataWrapper(clipId, email);
		
		logger.debug("Sharing: "+dataWrapper);
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

	private JSONObject prepareDataWrapper(String clipId, String email) throws JSONException
	{
		JSONObject dataWrapper = new JSONObject();
		JSONObject permissionsObject = new JSONObject();
		permissionsObject.put("clip", clipId);
		permissionsObject.put("recipient", email);

		dataWrapper.put("data", permissionsObject);
		return dataWrapper;
	}

	private URI preparePostURI() throws URISyntaxException
	{
		StringBuilder pathBuilder = new StringBuilder("/api/share");

		return HTTPUtils.buildURI("http", HTTPUtils.BASE_URL, pathBuilder.toString(), userManager);
	}
}
