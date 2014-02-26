package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.forTesting.TestingUtils;

/**
 * Uploads a browser media package to app engine.  Does not deal with sharing, that is
 * BrowserMediaPackageSharer
 * @author KevinLubick
 *
 */
public class BrowserMediaPackageUploader {

	
	private static final Logger logger = Logger.getLogger(BrowserMediaPackageUploader.class);
	
	private static CloseableHttpClient client = HttpClients.createDefault();
	
	
	//For whitebox/end-to-end testing
	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		String putUrl = "http://screencaster-hub.appspot.com/api/test@mailinator.com/Eclipse/Copy/4321/frame0000.jpg?";
		
		List<NameValuePair> userObject = assembleUserObject();
		putUrl+=URLEncodedUtils.format(userObject, "UTF-8");
		
		HttpPut httpPut = new HttpPut(putUrl);
		
		File file = new File("test_screencasting\\frame.36569190100000.jpg");
		
		
		
		MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();
		
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
	
	
	private static List<NameValuePair> assembleUserObject() throws JSONException
	{
		/*JSONObject userObject = new JSONObject();
		userObject.put("name", "Test User");
		userObject.put("email", "test@mailinator.com");
		userObject.put("token", "123");
		*/
		List<NameValuePair> retVal = new ArrayList<>();
		retVal.add(new BasicNameValuePair("name", "Test User"));
		retVal.add(new BasicNameValuePair("email", "test@mailinator.com"));
		retVal.add(new BasicNameValuePair("token", "123"));
		return retVal;
	}
	
	
}
