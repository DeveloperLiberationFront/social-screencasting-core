package edu.ncsu.lubick.externalAPI;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
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

	private String current_external_tool_id;

	private String current_external_clip_id;


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
			try
			{
				return uploadToolUsage(toolUsage, clipOptions, packageDirectory);
			}
			catch (IOException e)
			{
				logger.error("Problem uploading",e);
				return false;
			}
		}
		logger.error("Browser package not found, not uploading");
		return false;
	}


	private boolean uploadToolUsage(ToolUsage toolUsage, ClipOptions clipOptions, File packageDirectory) throws IOException
	{
		try
		{
			this.current_external_tool_id = getExternalToolId(toolUsage);

			logger.debug("Found external id "+current_external_tool_id);
			
			if (current_external_tool_id == null) {
				return false;
			}
			
			this.current_external_clip_id = makeExternalClip(toolUsage, packageDirectory);
			
			logger.info("Made clip id "+current_external_clip_id);
			
			File[] allFiles = packageDirectory.listFiles();
			return uploadCroppedFiles(allFiles, clipOptions);
		}
		catch (Exception e) {
			throw new IOException("Problem sharing", e);
		}
	}


	private String makeExternalClip(ToolUsage toolUsage, File packageDirectory) throws JSONException, URISyntaxException
	{
		JSONObject jobj = new JSONObject();
		jobj.put("name", packageDirectory.getName());
		jobj.put("tool", current_external_tool_id);
		if (ToolStream.MENU_KEY_PRESS.equals(toolUsage.getToolKeyPresses())) {
			jobj.put("type", "mouse");
		} else {
			jobj.put("type", "keyboard");
		}

		URI postUri = HTTPUtils.buildExternalHttpURI("/clips");

		HttpPost httpPost = new HttpPost(postUri);	
		HTTPUtils.addAuth(httpPost, userManager);
		
		String clipId = null;
		try
		{
			StringEntity content = new StringEntity(jobj.toString());
			content.setContentType("application/json");

			httpPost.setEntity(content);
			HttpResponse response = client.execute(httpPost);

			String responseBody = HTTPUtils.getResponseBody(response);
			logger.info("Reply: "+responseBody);
			
			JSONObject responseObj = new JSONObject(responseBody);
			clipId = responseObj.optString("_id");

		}
		catch (IOException e)
		{
			logger.error("Problem getting tool id",e);
		}
		finally {
			httpPost.reset();
		}
		return clipId;
	}


	private String getExternalToolId(ToolUsage toolUsage) throws JSONException, URISyntaxException
	{
		JSONObject jobj = new JSONObject();
		jobj.put("name", toolUsage.getToolName());
		jobj.put("application", toolUsage.getPluginName());

		URI getUri = new URIBuilder(HTTPUtils.buildExternalHttpURI("/tools")).addParameter("where",jobj.toString()).build();

		HttpGet httpGet = new HttpGet(getUri);	
		HTTPUtils.addAuth(httpGet, userManager);		
		String toolid = null;
		try
		{
			HttpResponse response = client.execute(httpGet);

			String responseBody = HTTPUtils.getResponseBody(response);
			logger.info("Reply: "+responseBody);
			
			JSONArray responseArray = new JSONObject(responseBody).optJSONArray("_items");
			if (responseArray != null && responseArray.length() > 0) {
				toolid = responseArray.getJSONObject(0).optString("_id");
			}
		}
		catch (IOException e)
		{
			logger.error("Problem getting tool id",e);
		}
		finally {
			httpGet.reset();
		}
		return toolid;
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
					if (!fileName.startsWith("frame")) {
						reportFile(file);
					}
					else if (fileNum >= startFrame && fileNum <= endFrame || fileNum == -1) {
						logger.info("Uploading: " + fileName);
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
			putUri = HTTPUtils.buildExternalHttpURI("/clips/"+current_external_clip_id+"/images");
		}
		catch (URISyntaxException e)
		{
			throw new IOException("Problem making the uri to send", e);
		}

		HttpPost httpPost = new HttpPost(putUri);
		HTTPUtils.addAuth(httpPost, userManager);
		try 
		{
			MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();

			BufferedImage image = ImageIO.read(imageFile);
			BufferedImage croppedImage = rect == null? image: image.getSubimage(rect.x, rect.y, rect.width, rect.height);
			
			
			ByteArrayOutputStream croppedJPG = new ByteArrayOutputStream();
			
			ImageIO.write(croppedImage, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, croppedJPG);
			
			byte[] croppedJPGForTransfer = croppedJPG.toByteArray();
			
			logger.debug("sending "+croppedJPGForTransfer.length + " bytes");
			
			mpeBuilder.addBinaryBody("data", croppedJPGForTransfer, ContentType.DEFAULT_BINARY, "");

			mpeBuilder.addTextBody("name", imageFile.getName());

			HttpEntity content = mpeBuilder.build();

			httpPost.setEntity(content);
			CloseableHttpResponse response = client.execute(httpPost);
			
			String responseBody = HTTPUtils.getResponseBody(response);
			logger.info("Reply: "+responseBody);
		}
		finally 
		{
			httpPost.reset();
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
					
					if (fileName.startsWith("frame")) {
						logger.info("Uploading: " + fileName);
						reportFile(file);
					}
					else if (!doesFileExist && (fileNum >= startFrame && fileNum <= endFrame || fileNum == -1))
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
			return Integer.parseInt(fileName.substring(fileName.indexOf('e') + 1, fileName.indexOf('.')));
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
			logger.info("files that exist on server "+responseBody);
			JSONObject clip = new JSONObject(responseBody);
			if (clip.has("error")) {
				return Collections.emptyList();
			}
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
	public static void main(String[] args) throws Exception
	{
		TestingUtils.makeSureLoggingIsSetUp();
		UserManager newManager = new UnitTestUserManager("Kevin Test","kjlubick+test@ncsu.edu","221ed3d8-6a09-4967-91b6-482783ec5313");
		BrowserMediaPackageUploader uploader = new BrowserMediaPackageUploader(newManager);
		Date toolUsageDate = new Date(7500L);
		IdealizedToolStream iToolStream = new IdealizedToolStream(TestingUtils.truncateTimeToMinute(toolUsageDate));
		iToolStream.addToolUsage("Save", "", "CTRL+5", toolUsageDate, 5500);

		ToolStream toolStream = ToolStream.generateFromJSON(iToolStream.toJSON());
		toolStream.setAssociatedPlugin("Eclipse");

		ToolUsage testToolUsage1 = toolStream.getAsList().get(0);

		ToolUsage testToolUsage = testToolUsage1;

		logger.info(uploader.uploadToolUsage(testToolUsage, new ClipOptions()));


	}


}
