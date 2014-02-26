package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.Runner;
import edu.ncsu.lubick.localHub.LocalHub;

/**
 * Uploads a browser media package to app engine
 * @author KevinLubick
 *
 */
public class BrowserMediaPackageUploader {

	
	private static final Logger logger = Logger.getLogger(BrowserMediaPackageUploader.class);
	
	private static CloseableHttpClient client = HttpClients.createDefault();
	
	static
	{
		try
		{
			URL url = Runner.class.getResource(LocalHub.LOGGING_FILE_PATH);
			PropertyConfigurator.configure(url);
			Logger.getRootLogger().info("Logging initialized");
		}
		catch (Exception e)
		{
			// load safe defaults
			BasicConfigurator.configure();
			Logger.getRootLogger().info("Could not load property file, loading defaults", e);
		}
	}
	
	//For whitebox/end-to-end testing
	public static void main(String[] args) throws Exception
	{
		
		String putUrl = "http://screencaster-hub.appspot.com/api/test@mailinator.com/Eclipse/Copy/4321/frame0000.jpg";
		
		HttpPut httpPut = new HttpPut(putUrl);
		
		File file = new File("test_screencasting\\frame.36569190100000.jpg");
		
		MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();
		
		JSONObject userObject = assembleUserObject();
		mpeBuilder.addTextBody("user", userObject.toString());
		
		mpeBuilder.addBinaryBody("image", file);

		try
		{
			HttpEntity content = mpeBuilder.build();

			httpPut.setEntity(content);
			client.execute(httpPut);
		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
	}
	
	
	private static JSONObject assembleUserObject() throws JSONException
	{
		JSONObject userObject = new JSONObject();
		userObject.put("name", "Test User");
		userObject.put("email", "test@mailinator.com");
		userObject.put("token", "123");
		return userObject;
	}
	
	
}
