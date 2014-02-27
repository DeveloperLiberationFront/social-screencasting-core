package edu.ncsu.lubick.localHub.videoPostProduction;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONException;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.http.HTTPUtils;
import edu.ncsu.lubick.unitTests.TestPostProductionHandler;
import edu.ncsu.lubick.util.FileUtilities;

/**
 * Uploads a browser media package to app engine.  Does not deal with sharing, that is
 * BrowserMediaPackageSharer
 * @author KevinLubick
 *
 */
public class BrowserMediaPackageUploader {

	
	private static final String BASE_URL = "http://screencaster-hub.appspot.com/api/";

	private static final Logger logger = Logger.getLogger(BrowserMediaPackageUploader.class);
	
	private CloseableHttpClient client = HttpClients.createDefault();
	
	private UserManager userManager = null;
	
	
	//For whitebox/end-to-end testing
	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		BrowserMediaPackageUploader uploader = new BrowserMediaPackageUploader();
		
		ToolUsage testToolUsage = TestPostProductionHandler.makeKeyboardToolUsage(new Date(7500L), "WhomboTool #1", 5500);
		
		uploader.uploadToolUsage(testToolUsage);
		
		
	}


	private ToolUsage currentToolUsage;


	private void uploadToolUsage(ToolUsage toolUsage)
	{
		String browserPackageRootDirName = FileUtilities.makeFolderNameForBrowserMediaPackage(toolUsage);
		
		setCurrentToolUsage(toolUsage);
		
		File packageDirectory = new File(browserPackageRootDirName);
		
		for(File file: packageDirectory.listFiles())
		{
			this.reportFile(file,file.getName());
		}
	}


	private void setCurrentToolUsage(ToolUsage toolUsage)
	{
		this.currentToolUsage = toolUsage;
	}


	private void reportFile(File file, String reportingName)
	{
		String putUrl = this.preparePutURL(reportingName);
		
		HttpPut httpPut = new HttpPut(putUrl);
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


	private String preparePutURL(String reportingName)
	{
		
		
		StringBuilder putUrl = new StringBuilder(BASE_URL);
		putUrl.append(userManager.getUserEmail());
		putUrl.append("/");
		putUrl.append(currentToolUsage.getPluginName());
		putUrl.append("/");
		putUrl.append(currentToolUsage.getToolName());
		putUrl.append("/");
		putUrl.append(currentToolUsage.getTimeStamp().getTime());
		putUrl.append("/");
		putUrl.append(reportingName);
		putUrl.append("?");

		putUrl.append(HTTPUtils.getUserAuthURL(userManager));
		return putUrl.toString();
	}
	
	
}
