package edu.ncsu.lubick.localHub.videoPostProduction;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
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


	private static final Logger logger = Logger.getLogger(BrowserMediaPackageUploader.class);
	
	private CloseableHttpClient client = HttpClients.createDefault();
	
	private UserManager userManager = null;
	
	
	public BrowserMediaPackageUploader(UserManager userManager)
	{
		this.userManager = userManager;
	}


	//For whitebox/end-to-end testing
	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		BrowserMediaPackageUploader uploader = new BrowserMediaPackageUploader(new UserManager(new File(".")));
		Date toolUsageDate = new Date(7500L);
		IdealizedToolStream iToolStream = new IdealizedToolStream(TestingUtils.truncateTimeToMinute(toolUsageDate));
		iToolStream.addToolUsage("Save", "", "CTRL+5", toolUsageDate, 5500);
		
		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin("Eclipse");
		assertEquals(1, toolStream.getAsList().size());
		
		ToolUsage testToolUsage1 = toolStream.getAsList().get(0);
		
		ToolUsage testToolUsage = testToolUsage1;
		
		uploader.uploadToolUsage(testToolUsage);
		
		
	}


	private ToolUsage currentToolUsage;


	private void uploadToolUsage(ToolUsage toolUsage)
	{
		String browserPackageRootDirName = FileUtilities.makeFolderNameForBrowserMediaPackage(toolUsage);
		
		setCurrentToolUsage(toolUsage);
		
		File packageDirectory = new File(browserPackageRootDirName);
		
		logger.info("Searching for browser package in directory "+packageDirectory);
		
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
		URI putUrl = this.preparePutURL(reportingName);
		
		HttpPut httpPut = new HttpPut(putUrl);
		MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();
		
		mpeBuilder.addBinaryBody("image", file);

		try
		{
			HttpEntity content = mpeBuilder.build();

			httpPut.setEntity(content);
			client.execute(httpPut);
			httpPut.abort();
			
		}
		catch (IOException e)
		{
			logger.error("Problem reporting tool info",e);
		}
	}


	private URI preparePutURL(String reportingName)
	{
		StringBuilder pathBuilder = new StringBuilder("/api/");
		pathBuilder.append(userManager.getUserEmail());
		pathBuilder.append("/");
		pathBuilder.append(currentToolUsage.getPluginName());
		pathBuilder.append("/");
		pathBuilder.append(currentToolUsage.getToolName());
		pathBuilder.append("/");
		pathBuilder.append(currentToolUsage.getTimeStamp().getTime());
		pathBuilder.append("/");
		pathBuilder.append(reportingName);
		
		URI u;
		try
		{
			u = new URI("http", HTTPUtils.BASE_URL, pathBuilder.toString(), HTTPUtils.getUnEscapedUserAuthURL(userManager), null);
			return u;
		}
		catch (URISyntaxException e)
		{
			logger.fatal("Could not encode URI",e);
			return null;
		}
		
		
	}
	
	
}
