package edu.ncsu.lubick.externalAPI;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
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
import edu.ncsu.lubick.util.ClipUtils;
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
	
	private ByteArrayOutputStream byteBufferForImage = new ByteArrayOutputStream();


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
				return uploadToolUsage(clipOptions, packageDirectory);
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


	private boolean uploadToolUsage(ClipOptions clipOptions, File packageDirectory) throws IOException
	{
		try
		{
			this.current_external_tool_id = getExternalToolId();

			logger.debug("Found external id "+current_external_tool_id);

			if (current_external_tool_id == null) {
				return false;
			}

			this.current_external_clip_id = makeExternalClip(packageDirectory, clipOptions);

			logger.info("Made clip id "+current_external_clip_id);
			if (current_external_clip_id == null) {
				return false;
			}

			File[] allFiles = packageDirectory.listFiles();
			return uploadAllFiles(allFiles, clipOptions);
		}
		catch (Exception e) {
			throw new IOException("Problem sharing", e);
		}
	}


	private String makeExternalClip(File packageDirectory, ClipOptions clipOptions) throws JSONException, URISyntaxException, IOException
	{
		JSONObject jobj = prepareClipObject(packageDirectory, clipOptions);
		 JSONObject postObj = postClipObject(jobj);
		
		 current_external_clip_id = postObj.optString("_id",null);
		if (current_external_clip_id == null) {
			return null;
		}
		
		//we have to do this in two steps, because the server doesn't handle lists well in conjunction with Multipart entities
		JSONObject thumbnailUpdateObj = reportThumbnail(packageDirectory, jobj);
		
		URI putUri = HTTPUtils.buildExternalHttpURI("/clips/"+current_external_clip_id);

		logger.info("Patching to include thumbnail " + thumbnailUpdateObj.toString(2));
		HttpPatch httpPatch = new HttpPatch(putUri);	
		HTTPUtils.addAuth(httpPatch, userManager);
		httpPatch.addHeader("If-Match",postObj.optString("_etag",null));
		try
		{
			StringEntity content = new StringEntity(thumbnailUpdateObj.toString());
			content.setContentType("application/json");

			httpPatch.setEntity(content);
			HttpResponse response = client.execute(httpPatch);

			String responseBody = HTTPUtils.getResponseBody(response);
			logger.info("Reply: "+responseBody);

		}
		catch (IOException e)
		{
			logger.error("Problem getting tool id",e);
		}
		finally {
			httpPatch.reset();
		}
		
		return current_external_clip_id;
	}


	private JSONObject reportThumbnail(File packageDirectory, JSONObject jobj) throws JSONException, IOException
	{
		JSONArray frameList = jobj.getJSONArray("frames");
		int eventFrame = jobj.getJSONArray("event_frames").getInt(0);
		logger.debug("Uploading thumbnail");
		String responseString = reportImageByteArray(ClipUtils.makeThumbnail(new File(packageDirectory, frameList.getString(eventFrame))),
		"thumbnail");		//no jpg needed
		JSONObject responseObject = new JSONObject(responseString);
		
		JSONObject returnObject = new JSONObject();
		returnObject.put("thumbnail", responseObject.optString("_id", null));
		return returnObject;
	}


	private JSONObject postClipObject(JSONObject jobj) throws URISyntaxException, JSONException
	{
		URI postUri = HTTPUtils.buildExternalHttpURI("/clips");

		HttpPost httpPost = new HttpPost(postUri);	
		HTTPUtils.addAuth(httpPost, userManager);

		try
		{
			StringEntity content = new StringEntity(jobj.toString());
			content.setContentType("application/json");

			httpPost.setEntity(content);
			HttpResponse response = client.execute(httpPost);

			String responseBody = HTTPUtils.getResponseBody(response);
			logger.info("Reply: "+responseBody);

			JSONObject responseObj = new JSONObject(responseBody);
			if (!"ERR".equals(responseObj.optString("_status","ERR"))) {
				return responseObj;
			}		
		}
		catch (IOException e)
		{
			logger.error("Problem getting tool id",e);
			
		}
		finally {
			httpPost.reset();
		}
		return null;
	}


	private JSONObject prepareClipObject(File packageDirectory, ClipOptions clipOptions) throws JSONException
	{
		JSONObject jobj = new JSONObject();
		jobj.put("name", packageDirectory.getName());
		jobj.put("tool", current_external_tool_id);
		if (ToolStream.MENU_KEY_PRESS.equals(currentToolUsage.getToolKeyPresses())) {
			jobj.put("type", "mouse");
		} else {
			jobj.put("type", "keyboard");
		}
		JSONArray emailJarr = new JSONArray();
		emailJarr.put(clipOptions.shareWithEmail);
		
		//by default, the action happens 5 seconds after the beginning of the clip
		//Cropping this 
		int eventFrame = 5 * PostProductionHandler.FRAME_RATE - clipOptions.startFrame;
		
		
		JSONArray eventFrames = new JSONArray().put(eventFrame);
		jobj.put("event_frames", eventFrames);
		jobj.put("share", emailJarr);
		
		JSONArray frameList = ClipUtils.makeFrameListForClip(packageDirectory, clipOptions.startFrame, clipOptions.endFrame);
		jobj.put("frames", frameList);
		return jobj;
	}


	private String getExternalToolId() throws JSONException, URISyntaxException
	{
		JSONObject jobj = new JSONObject();
		jobj.put("name", currentToolUsage.getToolName());
		jobj.put("application", currentToolUsage.getPluginName());

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

	private boolean uploadAllFiles(File[] files, ClipOptions clipOptions)
	{
		int startFrame = clipOptions.startFrame;
		int endFrame = clipOptions.endFrame > 0 ? clipOptions.endFrame : files.length;
		for(File file : files)
		{
			String fileName = file.getName();
			try
			{

				if (!fileName.startsWith("frame")) {
					logger.info("Uploading: " + fileName);
					reportUncroppedImage(file);
					continue;
				}
				try
				{
					int fileNum = fileNameToInt(fileName);
					if (fileNum >= startFrame && fileNum <= endFrame || fileNum == -1) {
						logger.info("Uploading: " + fileName);
						reportCroppedImage(file, clipOptions.cropRect);
					}
				}
				catch(NumberFormatException e)
				{
					logger.info("Uploading: " + file.getAbsolutePath());
					reportUncroppedImage(file);
				}
			}
			catch(IOException e)
			{
				logger.fatal("Could not report file " + file.getAbsolutePath(), e);
				return false;
			}
		}

		logger.info("Done uploading files");
		return true;
	}

	private void reportUncroppedImage(File imageFile) throws IOException
	{
		reportCroppedImage(imageFile, null);
	}


	private void reportCroppedImage(File imageFile, Rectangle rect) throws IOException
	{
		try 
		{
			BufferedImage image = ImageIO.read(imageFile);
			BufferedImage croppedImage = rect == null? image: image.getSubimage(rect.x, rect.y, rect.width, rect.height);

			ImageIO.write(croppedImage, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, byteBufferForImage);

			byte[] croppedJPGForTransfer = byteBufferForImage.toByteArray();

			this.reportImageByteArray(croppedJPGForTransfer, imageFile.getName());
		}
		finally 
		{
			byteBufferForImage.reset();
		}
	}
	
	private String reportImageByteArray(byte[] imageByteArray, String fileName) throws IOException {
		URI postUri;
		try
		{
			postUri = HTTPUtils.buildExternalHttpURI("/clips/"+current_external_clip_id+"/images");
		}
		catch (URISyntaxException e)
		{
			throw new IOException("Problem making the uri to send", e);
		}

		HttpPost httpPost = new HttpPost(postUri);
		HTTPUtils.addAuth(httpPost, userManager);
		try 
		{
			MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();
			
			logger.debug("sending "+imageByteArray.length + " bytes");
			mpeBuilder.addBinaryBody("data", imageByteArray, ContentType.DEFAULT_BINARY, "");

			mpeBuilder.addTextBody("name", fileName);

			HttpEntity content = mpeBuilder.build();

			httpPost.setEntity(content);
			CloseableHttpResponse response = client.execute(httpPost);

			String responseBody = HTTPUtils.getResponseBody(response);
			logger.info("Reply: "+responseBody);
			return responseBody;
		}
		finally 
		{
			httpPost.reset();
		}
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

	private void setCurrentToolUsage(ToolUsage toolUsage)
	{
		this.currentToolUsage = toolUsage;
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

		ToolUsage testToolUsage = toolStream.getAsList().get(0); 
		
		logger.info(FileUtilities.makeLocalFolderNameForBrowserMediaPackage(testToolUsage, newManager.getUserEmail()));

		logger.info(uploader.uploadToolUsage(testToolUsage, new ClipOptions("public", 2, 0)));


	}


}
