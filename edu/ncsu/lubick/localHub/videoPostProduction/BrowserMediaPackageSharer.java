package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.lubick.localHub.http.HTTPUtils;

public class BrowserMediaPackageSharer {
	
	
	private static final Logger logger = Logger.getLogger(BrowserMediaPackageSharer.class);
	private static CloseableHttpClient client = HttpClients.createDefault();
	private static UserManager userManager;

	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		userManager = new UnitTestUserManager("Test User", "test@mailinator.com", "123");
		
		URI putUrl = preparePutURI();
		
		HttpPost httpPut = new HttpPost(putUrl);
		
		JSONObject dataWrapper = new JSONObject();
		JSONObject permissionsObject = new JSONObject();
		permissionsObject.put("clip", "Eclipsedc3a37d4-4469-391f-bd62-0324ac2b7091");
		permissionsObject.put("recipient", "test2@mailinator.com");
		dataWrapper.put("data", permissionsObject);
		
		logger.debug("Sharing: "+dataWrapper.toString());
		try
		{
			StringEntity content = new StringEntity(dataWrapper.toString());
			content.setContentType("application/json");

			httpPut.setEntity(content);
			HttpResponse response = client.execute(httpPut);
			
			logger.info("Reply: "+HTTPUtils.getResponseBody(response));
		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
		
	}

	private static URI preparePutURI() throws URISyntaxException
	{
		StringBuilder pathBuilder = new StringBuilder("/api/share");

		URI u = new URI("http", HTTPUtils.BASE_URL, pathBuilder.toString(), HTTPUtils.getUnEscapedUserAuthURL(userManager), null);
		return u;
	}
}
