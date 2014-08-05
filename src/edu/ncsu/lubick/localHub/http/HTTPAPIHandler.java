package edu.ncsu.lubick.localHub.http;

import static edu.ncsu.lubick.localHub.http.HTTPUtils.*;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.WebQueryInterface;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.util.FileUtilities;
import edu.ncsu.lubick.util.ToolCountStruct;

public class HTTPAPIHandler extends AbstractHandler {

	private static final Logger logger = Logger.getLogger(HTTPAPIHandler.class);
	private WebQueryInterface databaseLink;
	private UserManager userManager = HTTPServer.getUserManager();

	private ByteArrayOutputStream byteBufferForImage = new ByteArrayOutputStream();
	private Map<String, String[]> queryParams;
	private HTTPClipSharer clipSharer; 

	public HTTPAPIHandler(WebQueryInterface wqi)
	{
		this.databaseLink = wqi;
		clipSharer = new HTTPClipSharer(wqi);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!(target.startsWith("/api"))) {
			return;
		}
		baseRequest.setHandled(true);
		response.setContentType("application/json");
		
		this.queryParams = request.getParameterMap();

		String type = baseRequest.getMethod();

		if ("POST".equals(type))
		{	
			//pass this off to clipSharer
			if (target.startsWith("/api/shareClip")) {
				clipSharer.handle(target, baseRequest, request, response);
			}
			else {
				logger.error("I don't know how to handle a POST like this " + target);
				response.getWriter().println("Sorry, Nothing at this URL");
			}
		}
		else if (target.length() < 5)			
		{
			response.getWriter().println("Sorry, Nothing at this URL");
		}
		else
		{
			handleGet(target, response);
		}
	}

	private void handleGet(String target, HttpServletResponse response) throws IOException
	{
		String[] pieces = target.split("/");
		/*
		 * /api/:creator/:app/:tool/:clip-name/'
		 * pieces[0] will be an empty string
		 * pieces[1] will be the the string "api" or "clips"
		 */
		logger.info("Broken up target "+Arrays.toString(pieces));
		if (pieces.length <= 2) {
			response.getWriter().println("Sorry, Nothing at this URL");
		} else if ("clips".equals(pieces[2])){ 
			handleClipsAPI(pieces, response);
		} else if (pieces.length == 3){		//api/user
			handleGetUserInfo(chopOffQueryString(pieces[2]), response);
		} else {
			response.getWriter().println("Sorry, Nothing at this URL");
		}
	}
	
	private void handleClipsAPI(String[] pieces, HttpServletResponse response) throws IOException
	{
		if (pieces.length == 3) {
			//just "/api/clips"
			handleGetClipsForTool(response);
			return;
		}
		
		//"/api/clips/[clip_id]/images"
		//"return {name , data}"
		if (pieces.length != 5) {
			response.getWriter().println("Sorry, Nothing at this URL");
			return;
		}
		
		String clipId = pieces[3];
		
		JSONArray frameList = makeFrameListForClip(new File("renderedVideos",clipId));
		
		try
		{
			JSONArray returnArray = new JSONArray();
			
			for(int i = 0;i< frameList.length(); i++) {
				File file = new File("renderedVideos/"+clipId, frameList.getString(i));
				logger.info(file.getAbsolutePath());
				BufferedImage img = ImageIO.read(file);

				ImageIO.write(img, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, byteBufferForImage);

				byte[] imageAsBytes = byteBufferForImage.toByteArray();
				
				
				JSONObject jobj = new JSONObject();
				jobj.put("name", "frame"+FileUtilities.padIntTo4Digits(i)+".jpg");
				jobj.put("data", Base64.encodeBase64String(imageAsBytes));


				returnArray.put(jobj);
				byteBufferForImage.reset();
			}
			
			returnArray.write(response.getWriter());
		}
		catch (JSONException e) {
			throw new IOException("Problem encoding image" , e);
		}
		finally {
			byteBufferForImage.reset();
		}
		
	}

	private void handleGetClipsForTool(HttpServletResponse response) throws IOException
	{
		String application = null;
		String tool = null;
		try
		{
			application = queryParams.get("app")[0];
			tool = queryParams.get("tool")[0];
		}
		catch (NullPointerException | ArrayIndexOutOfBoundsException e)
		{
			logger.error("Malformed query params, no app or tool params: "+queryParams, e);
			response.getWriter().println("Malformed query params, no app or tool params");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	
		replyWithAllClipsForTool(application, tool, response);
	}

	private void replyWithAllClipsForTool(String applicationName, String toolName, HttpServletResponse response) throws IOException
	{
		List<File> clips = databaseLink.getBestExamplesOfTool(applicationName, toolName, true);
		clips.addAll(databaseLink.getBestExamplesOfTool(applicationName, toolName, false));
		
		//XXX sample data
		clips.add(new File("renderedVideos/Eclipse16141cfc-87cb-32dc-bc30-fedcad3b7598G"));
		
		JSONArray jarr = new JSONArray();
		for(File clip: clips) {
			JSONArray fileNamesArr = makeFrameListForClip(clip);
			if (fileNamesArr == null) {
				logger.info("Clip "+clip.getName()+" does not exist");
				response.getWriter().println("Could not find clip "+clip.getName());
				return;
			}
	
			JSONObject clipObject = new JSONObject();
			try{	
				clipObject.put("frames", fileNamesArr);
				clipObject.put("name", clip.getName());
				clipObject.put("app", applicationName);
				clipObject.put("tool", toolName);
				clipObject.put("creator", userManager.getUserEmail());		
				clipObject.put("thumbnail", makeThumbnail(clip.getName(), fileNamesArr));
				jarr.put(clipObject);
			}
			catch (JSONException e)
			{
				logger.error("Problem compiling clip names and writing them out "+clipObject,e);
				response.getWriter().println("There was a problem compiling the clip details");
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
		
		response.setContentType("application/json");
		try {
			jarr.write(response.getWriter());
		} catch (JSONException e) {	
			response.getWriter().println("There was a problem compiling the clip details");
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw new IOException("Problem writing to response", e);
		}
		
	
		
	}

	private void handleGetUserInfo(String emailTarget, HttpServletResponse response) throws IOException
	{
		if (emailTarget.equals(userManager.getUserEmail()) || "user".equals(emailTarget) || "users".equals(emailTarget)) 
		{
			try {
				JSONObject user = makeUserAuthObj();

				user.write(response.getWriter());
			}
			catch (JSONException e) {
				throw new IOException("Problem making JSON", e);
			}
		} 
		else 
		{
			response.getWriter().println("You can only query the local hub for information about you, "+ userManager.getUserName());
		}

	}

	private JSONObject makeUserAuthObj() throws JSONException
	{
		JSONObject user = new JSONObject();
		user.put("email", userManager.getUserEmail());
		user.put("name", userManager.getUserName());
		user.put("token", userManager.getUserToken());
		return user;
	}

	private String makeThumbnail(String clipId, JSONArray fileNamesArr)
	{
		try {
			int indexForThumbnail = fileNamesArr.length() > 25? 25 : fileNamesArr.length()/2;

			File file = new File("renderedVideos/"+clipId, fileNamesArr.getString(indexForThumbnail));
			logger.info(file.getAbsolutePath());
			BufferedImage img = ImageIO.read(file);

			int newWidth = (img.getWidth() * 200) / img.getHeight();
			BufferedImage scaledImage = new BufferedImage(newWidth, 200, BufferedImage.TYPE_INT_RGB);

			scaledImage.createGraphics().drawImage(img, 0, 0, newWidth, 200, 0, 0, img.getWidth(), img.getHeight(), null);

			byteBufferForImage.reset();
			//ImageIO.write(scaledImage, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, new FileOutputStream("./test.jpg"));
			ImageIO.write(scaledImage, PostProductionHandler.INTERMEDIATE_FILE_FORMAT, byteBufferForImage);

			return Base64.encodeBase64String(byteBufferForImage.toByteArray());
		}
		catch (IOException | JSONException e)
		{
			logger.error("Problem making a thumbnail " + clipId + " " + fileNamesArr);
			return null;
		}
		finally {
			byteBufferForImage.reset();
		}
	}

	public static JSONArray makeFrameListForClip(File clipDir) 
	{
		JSONArray fileNamesArr = new JSONArray();

		if (clipDir.exists() && clipDir.isDirectory())
		{
			String[] files = clipDir.list();
			Arrays.sort(files);
			for(String imageFile: files)
			{
				if (imageFile.startsWith("frame"))
					fileNamesArr.put(imageFile);
			}
		}
		else {
			return null;
		}
		return fileNamesArr;
	}

	private void handleImageRequest(String clipId, String fileName, HttpServletResponse response) throws IOException
	{
		response.sendRedirect("/"+clipId+"/"+fileName);
	}

	private void handleGetAllToolsForPlugin(String applicationName, HttpServletResponse response) throws IOException
	{
		JSONObject dataObj = new JSONObject();
		JSONArray appArr = makePluginArray(applicationName);
		try {
			dataObj.put("tools", appArr);
			dataObj.write(response.getWriter());
		}
		catch (JSONException e) {
			throw new IOException("Problem making JSON " + dataObj, e);
		}
	
	}

	private void handleGetInfoAboutTool(String applicationName, String toolName, HttpServletResponse response) throws IOException
	{
		List<File> keyClips = databaseLink.getBestExamplesOfTool(applicationName, toolName, true);
		List<File> guiClips = databaseLink.getBestExamplesOfTool(applicationName, toolName, false);
		ToolCountStruct countStruct = databaseLink.getToolAggregate(applicationName, toolName);
	
		JSONObject clips = new JSONObject();
	
		try
		{
			JSONArray keyJarr = new JSONArray();
			for(File f: keyClips)
			{
				keyJarr.put(f.getName());
			}
	
			JSONArray guiJarr = new JSONArray();
			for(File f: guiClips)
			{
				guiJarr.put(f.getName());
			}
	
			JSONObject usage = new JSONObject();
			JSONObject toolJson = new JSONObject();
			toolJson.put("gui", countStruct.guiToolCount);
			toolJson.put("keyboard", countStruct.keyboardCount);
			usage.put(toolName, toolJson);
	
			// Testing data
			keyJarr.put("Eclipse16274d13-bebb-3196-832c-70313e08cdaaK");
			keyJarr.put("Eclipsea3aabc7a-d2dc-33d1-84a7-066372cb4d73K");
			guiJarr.put("Eclipse16141cfc-87cb-32dc-bc30-fedcad3b7598G");
			guiJarr.put("Eclipse29bf2b83-2e3d-3855-9286-ee7f69db64c1G");
			guiJarr.put("Eclipse13d5a993-e46f-3b7f-862a-bfefa5831901G");
	
	
			clips.put("keyclips",keyJarr);
			clips.put("guiclips",guiJarr);
			clips.put("usage", usage);
	
			response.setContentType("application/json");
			clips.write(response.getWriter());
		}
		catch (JSONException e)
		{
			logger.error("Problem compiling clip names and writing them out "+clips,e);
		}
	}

	private JSONArray makePluginArray(String pluginName)
	{
		List<ToolCountStruct> counts = databaseLink.getAllToolAggregateForPlugin(pluginName);
	
		JSONArray retVal = new JSONArray();
		for(ToolCountStruct tcs: counts)
		{
			JSONObject tempObject = new JSONObject();
			try
			{
				tempObject.put("clips", 5);		//TODO FIX
				tempObject.put("gui", tcs.guiToolCount);
				tempObject.put("keyboard", tcs.keyboardCount);
				tempObject.put("name", tcs.toolName);
				retVal.put(tempObject);
			}
			catch (JSONException e)
			{
				logger.error("Unusual JSON exception, squashing: "+tempObject,e);
			}
		}
		return retVal;
	}

}
