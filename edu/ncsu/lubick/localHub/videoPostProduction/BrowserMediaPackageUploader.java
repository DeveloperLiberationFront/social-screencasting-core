package edu.ncsu.lubick.localHub.videoPostProduction;

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
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.lubick.localHub.http.HTTPUtils;
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
	private ToolUsage currentToolUsage;


	public BrowserMediaPackageUploader(UserManager userManager)
	{
		this.userManager = userManager;
	}


	public boolean uploadToolUsage(ToolUsage toolUsage)
	{
		String expectedLocationOnDisk = FileUtilities.makeLocalFolderNameForBrowserMediaPackage(toolUsage, userManager.getUserEmail());

		setCurrentToolUsage(toolUsage);

		File packageDirectory = new File(expectedLocationOnDisk);

		logger.info("Searching for browser package in directory "+packageDirectory);

		if (packageDirectory.exists()&&packageDirectory.isDirectory())
		{
			int counter = 1, totalFiles = packageDirectory.listFiles().length;
			for(File file: packageDirectory.listFiles())
			{
				try
				{
					logger.info(String.format("reporting file %d/%d",counter,totalFiles));
					this.reportFile(file,file.getName());
					counter++;
				}
				catch (IOException e)
				{
					logger.fatal("Could not upload browser package.  Problem with file "+counter +"("+file+")",e);
					return false;
				}
			}
			return true;
		}
		logger.error("Browser package not found, not uploading");
		return false;
	}


	private void setCurrentToolUsage(ToolUsage toolUsage)
	{
		this.currentToolUsage = toolUsage;
	}


	private void reportFile(File file, String reportingName) throws IOException
	{
		URI putUri;
		try
		{
			putUri = this.preparePutURI(reportingName);
		}
		catch (URISyntaxException e)
		{
			throw new IOException("Problem making the uri to send", e);
		}

		HttpPut httpPut = new HttpPut(putUri);
		MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();

		mpeBuilder.addBinaryBody("image", file);

		HttpEntity content = mpeBuilder.build();

		httpPut.setEntity(content);
		client.execute(httpPut);
		httpPut.abort();

	}


	private URI preparePutURI(String reportingName) throws URISyntaxException
	{
		StringBuilder pathBuilder = new StringBuilder("/api/");
		pathBuilder.append(userManager.getUserEmail());
		pathBuilder.append("/");
		pathBuilder.append(currentToolUsage.getPluginName());
		pathBuilder.append("/");
		pathBuilder.append(currentToolUsage.getToolName());
		pathBuilder.append("/");
		pathBuilder.append(ToolStream.makeUniqueIdentifierForToolUsage(currentToolUsage, userManager.getUserEmail()));
		pathBuilder.append("/");
		pathBuilder.append(reportingName);

		URI u = new URI("http", HTTPUtils.BASE_URL, pathBuilder.toString(), HTTPUtils.getUnEscapedUserAuthURL(userManager), null);
		return u;
	}


	//For whitebox/end-to-end testing
	@SuppressWarnings("unused")
	private static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		UserManager newManager = new UnitTestUserManager("Test User","test@mailinator.com","123");
		BrowserMediaPackageUploader uploader = new BrowserMediaPackageUploader(newManager);
		Date toolUsageDate = new Date(7500L);
		IdealizedToolStream iToolStream = new IdealizedToolStream(TestingUtils.truncateTimeToMinute(toolUsageDate));
		iToolStream.addToolUsage("Save", "", "CTRL+5", toolUsageDate, 5500);

		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin("Eclipse");

		ToolUsage testToolUsage1 = toolStream.getAsList().get(0);

		ToolUsage testToolUsage = testToolUsage1;

		uploader.uploadToolUsage(testToolUsage);


	}


}
