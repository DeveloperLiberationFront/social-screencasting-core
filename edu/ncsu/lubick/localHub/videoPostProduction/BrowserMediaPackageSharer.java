package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.IOException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.forTesting.TestingUtils;

public class BrowserMediaPackageSharer {
	
	
	private static final Logger logger = Logger.getLogger(BrowserMediaPackageSharer.class);
	private static CloseableHttpClient client = HttpClients.createDefault();

	public static void main(String[] args)
	{
		TestingUtils.makeSureLoggingIsSetUp();
		
		String putUrl = "http://screencaster-hub.appspot.com/api/test@mailinator.com/Eclipse/Copy/4321/";
		
		HttpPut httpPut = new HttpPut(putUrl);
		
		//TODO permissions
		JSONObject permissionsObject = new JSONObject();
		
		try
		{
			StringEntity content = new StringEntity(permissionsObject.toString());
			content.setContentType("application/json");

			httpPut.setEntity(content);
			client.execute(httpPut);
		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
		
	}
}
