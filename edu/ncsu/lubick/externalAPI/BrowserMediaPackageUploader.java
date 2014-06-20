package edu.ncsu.lubick.externalAPI;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.ClipOptions;
import edu.ncsu.lubick.localHub.ToolStream;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.forTesting.IdealizedToolStream;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.lubick.localHub.http.HTTPUtils;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
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


	public boolean uploadToolUsage(ToolUsage toolUsage, ClipOptions clipOptions)
	{
		String expectedLocationOnDisk = FileUtilities.makeLocalFolderNameForBrowserMediaPackage(toolUsage, userManager.getUserEmail());

		setCurrentToolUsage(toolUsage);

		File packageDirectory = new File(expectedLocationOnDisk);

		logger.info("Searching for browser package in directory "+packageDirectory);
		
		if (packageDirectory.exists() && packageDirectory.isDirectory())
		{
			return uploadFile(toolUsage, clipOptions, packageDirectory);
		}
		logger.error("Browser package not found, not uploading");
		return false;
	}


	private boolean uploadFile(ToolUsage toolUsage, ClipOptions clipOptions, File packageDirectory)
	{
		File[] allFiles = packageDirectory.listFiles();
		
		if (clipOptions.cropRect != null) {
			List<String> existingFiles = getExistingFilesOnExternalServer(toolUsage);

			return incrementalUploadFiles(allFiles, existingFiles, clipOptions); 
		}
		
		return uploadCroppedFiles(allFiles, clipOptions);
	}
	
	private boolean uploadCroppedFiles(File[] files, ClipOptions clipOptions)
	{
		int startFrame = clipOptions.startFrame;
		int endFrame = clipOptions.endFrame > 0 ? clipOptions.endFrame : files.length;
		for(File file : files)
		{
			String fileName = file.getName();
			try
			{
				try
				{
					int fileNum = fileNameToInt(fileName);
					if (fileNum >= startFrame && fileNum <= endFrame || fileNum == -1) {
						reportCroppedImage(file, clipOptions.cropRect);
					}
				}
				catch(NumberFormatException e)
				{
					logger.info("Uploading: " + file.getAbsolutePath());
					reportFile(file);
				}
			}
			catch(IOException e)
			{
				logger.fatal("Could not report/unreport file " + file.getAbsolutePath(), e);
				return false;
			}
		}
		
		logger.info("Done uploading/unuploading files");
		return true;
	}
	
	private void reportCroppedImage(File imageFile, Rectangle rect) throws IOException
	{
		URI putUri;
		try
		{
			putUri = this.preparePutURI(imageFile.getName());
		}
		catch (URISyntaxException e)
		{
			throw new IOException("Problem making the uri to send", e);
		}

		HttpPut httpPut = new HttpPut(putUri);
		try 
		{
			MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();

			BufferedImage image = ImageIO.read(imageFile);
			BufferedImage croppedImage = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
			
			ByteArrayOutputStream croppedJPG = new ByteArrayOutputStream();
			
			ImageIO.write(croppedImage, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, croppedJPG);
			
			ByteArrayInputStream croppedJPGForTransfer = new ByteArrayInputStream(croppedJPG.toByteArray());
			
			mpeBuilder.addBinaryBody("image", croppedJPGForTransfer);

			HttpEntity content = mpeBuilder.build();

			httpPut.setEntity(content);
			client.execute(httpPut);
		}
		finally 
		{
			httpPut.reset();
		}

	}
		
	private boolean incrementalUploadFiles(File[] files, List<String> existingFiles, ClipOptions clipOptions)
	{
		int startFrame = clipOptions.startFrame;
		int endFrame = clipOptions.endFrame > 0 ? clipOptions.endFrame : files.length;
		for(File file : files)
		{
			String fileName = file.getName();
			int fileNum = -1;
			
			try
			{
				try
				{
					fileNum = fileNameToInt(fileName);
					boolean doesFileExist = existingFiles.contains(fileName);
				
					if (!doesFileExist && (fileNum >= startFrame && fileNum <= endFrame || fileNum == -1))
					{
						logger.info("Uploading: " + fileName);
						reportFile(file);
					}
					else if (doesFileExist && (fileNum != -1 && (fileNum < startFrame || fileNum > endFrame)))
					{
						logger.info("Unuploading: " + fileName);
						unreportFile(file);
					}
				}
				catch(NumberFormatException e)
				{
					logger.info("Uploading: " + file.getAbsolutePath());
					reportFile(file);
				}
			}
			catch(IOException e)
			{
				logger.fatal("Could not report/unreport file " + file.getAbsolutePath(), e);
				return false;
			}
		}
		
		logger.info("Done uploading/unuploading files");
		return true;
	}

	private int fileNameToInt(String fileName)
	{
		try
		{
			return Integer.parseInt(fileName.substring(fileName.indexOf("e") + 1, fileName.indexOf(".")));
		}
		catch(NumberFormatException e)
		{
			return -1;
		}
	}

	private List<String> getExistingFilesOnExternalServer(ToolUsage toolUsage) {
		List<String> existingFiles = null;
		
		URI getUri = makeURIForExistingFiles(toolUsage);

		HttpGet httpGet = new HttpGet(getUri);
		try
		{
			CloseableHttpResponse response = client.execute(httpGet);
			
			String responseBody = HTTPUtils.getResponseBody(response);
			JSONObject clip = new JSONObject(responseBody);
			JSONArray filenames = clip.getJSONObject("clip").getJSONArray("filenames");
			
			existingFiles = new ArrayList<>(filenames.length());
			
			for(int i=0; i<filenames.length(); i++)
			{
				existingFiles.add(filenames.getString(i));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			httpGet.reset();
		}
		
		
		return existingFiles;
	}


	private URI makeURIForExistingFiles(ToolUsage toolUsage)
	{
		URI getUri = null;
		try
		{
			StringBuilder sb = new StringBuilder("/api/");
			sb.append(userManager.getUserEmail());
			sb.append("/");
			sb.append(toolUsage.getPluginName());
			sb.append("/");
			sb.append(toolUsage.getToolName());
			sb.append("/");
			sb.append(ToolStream.makeUniqueIdentifierForToolUsage(toolUsage, userManager.getUserEmail()));
			getUri = HTTPUtils.buildExternalHttpURI(sb.toString(), userManager);
			logger.info(getUri);
			
		} catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
		return getUri;
	}


	private void setCurrentToolUsage(ToolUsage toolUsage)
	{
		this.currentToolUsage = toolUsage;
	}


	private void reportFile(File file) throws IOException
	{
		URI putUri;
		try
		{
			putUri = this.preparePutURI(file.getName());
		}
		catch (URISyntaxException e)
		{
			throw new IOException("Problem making the uri to send", e);
		}

		HttpPut httpPut = new HttpPut(putUri);
		try 
		{
			MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();

			mpeBuilder.addBinaryBody("image", file);

			HttpEntity content = mpeBuilder.build();

			httpPut.setEntity(content);
			client.execute(httpPut);
		}
		finally 
		{
			httpPut.reset();
		}

	}
	
	private void unreportFile(File file) throws IOException
	{
		URI deleteUri;
		try
		{
			deleteUri = preparePutURI(file.getName());
		}
		catch (URISyntaxException e)
		{
			throw new IOException("Problem making the uri to send", e);
		}

		HttpDelete request = new HttpDelete(deleteUri);
		try
		{
			logger.debug(request.getURI());
			client.execute(request);
		}
		finally
		{
			request.reset();
		}
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

		return HTTPUtils.buildExternalHttpURI(pathBuilder.toString(), userManager);
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

		uploader.uploadToolUsage(testToolUsage, new ClipOptions());


	}


}
