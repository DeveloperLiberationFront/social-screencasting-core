package edu.ncsu.lubick.localHub.http;

import static edu.ncsu.lubick.localHub.http.HTTPUtils.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

import edu.ncsu.lubick.Runner;
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

	public HTTPAPIHandler(WebQueryInterface wqi)
	{
		this.databaseLink = wqi;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!target.startsWith("/api") && !target.startsWith("/clips")) {
			return;
		}
		baseRequest.setHandled(true);
		response.setContentType("application/json");
		
		this.queryParams = request.getParameterMap();

		String type = baseRequest.getMethod();

		if ("POST".equals(type))
		{
			logger.error("I don't know how to handle a POST like this");
			response.getWriter().println("Sorry, Nothing at this URL");
		}
		else if (target.length() < 5)			
		{
			response.getWriter().println("Sorry, Nothing at this URL");
		}
		else if ("GET".equals(type))
		{
			handleGet(target, response);
		}else if("PUT".equals(type)){
			handlePUT(target,request,response);
		}
	}
	
	private void handlePUT(String target, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		String[] pieces = target.split("/");

		if ("status".equals(pieces[2])){
			handlePUTStatus(pieces,request,response);
		}else {	
			response.getWriter().println("Sorry, Nothing at this URL");
		}
	}
	private void handlePUTStatus(String[] pieces, HttpServletRequest request,
		HttpServletResponse response) throws IOException {	
		if ("recording".equals(pieces[3])){
			handlePUTRecording(request, response);
		}else {
			response.getWriter().println("Sorry, Nothing at this URL");
		}
	}
	
	private void handlePUTRecording(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		try {
			JSONObject jsonObject = HTTPUtils.getRequestJSON(request);
			databaseLink.userPause(jsonObject.getBoolean("status"));
		} catch (JSONException e) {
			response.sendError(500);
			e.printStackTrace();
		}
	}
	
	private void handleGet(String target, HttpServletResponse response) throws IOException
	{
		String[] pieces = target.split("/");
		/*
		 * /api/:creator/:app/:tool/:clip-name/'
		 * pieces[0] will be an empty string
		 * pieces[1] will be the the string "api" 
		 * pieces[2] could be "clips" or "status"
		 */
		logger.info("Broken up target "+Arrays.toString(pieces));
		if (pieces.length <= 2) {
			response.getWriter().println("Sorry, Nothing at this URL");
		} else if ("clips".equals(pieces[2])){ 
			handleClipsAPI(pieces, response);
		}else if("status".equals(pieces[2])){
			handleGetStatus(pieces, response);
		}else if (pieces.length == 3){
			handleGetUserInfo(chopOffQueryString(pieces[2]), response);
		} else if (pieces.length == 4) {
			handleGetAllToolsForPlugin(chopOffQueryString(pieces[3]), response);
		} else if (pieces.length == 5) {
			handleGetInfoAboutTool(pieces[3], chopOffQueryString(pieces[4]), response);
		} else if (pieces.length == 6) {
			//handleGetClipInfo(pieces[3], pieces[4], chopOffQueryString(pieces[5]), response);
			response.getWriter().println("Sorry, This url was deprecated");
		} else if (pieces.length == 7) {
			handleImageRequest(pieces[5], chopOffQueryString(pieces[6]), response);
		} else {
			response.getWriter().println("Sorry, Nothing at this URL");
		}
	}
	
	private void handleGetStatus(String[] pieces, HttpServletResponse response)
			throws IOException {
			if ("recording".equals(pieces[3])){
				handleGetRecording(response);
			}else {
				response.getWriter().println("Sorry, Nothing at this URL");
			}
	}
	
	private void handleGetRecording(HttpServletResponse response)
			throws IOException {
		try {
			JSONObject status = makeRecordingObj();
			status.write(response.getWriter());
		} catch (JSONException e) {
			throw new IOException("Problem making JSON", e);
		}
	}
	
	
	private void handleClipsAPI(String[] pieces, HttpServletResponse response) throws IOException
	{
		if (pieces.length == 3) {
			//just "/api/clips"
			//TODO null checks
			String application = queryParams.get("app")[0];
			String tool = queryParams.get("tool")[0];
			
			handleGetClipInfo(application, tool, response);
			return;
		}
		
		//"/api/clips/[clip_id]/images"
		//"return {name , data}"
		if (pieces.length != 5) {
			response.getWriter().println("Sorry, Nothing at this URL");
			return;
		}
		
		String clipId = pieces[3];
		
		JSONArray frameList = makeFrameList(new File("renderedVideos",clipId));
		
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

	private void handleGetUserInfo(String emailTarget, HttpServletResponse response) throws IOException
	{
		if (emailTarget.equals(userManager.getUserEmail())  || "user".equals(emailTarget) || "users".equals(emailTarget)) 
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

	private JSONObject makeRecordingObj() throws JSONException {
		JSONObject status = new JSONObject();
		status.put("id", "recording");
		status.put("status", Runner.isRunning());
		return status;
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

	private void handleGetClipInfo(String applicationName, String toolName, HttpServletResponse response) throws IOException
	{
		List<File> clips = databaseLink.getBestExamplesOfTool(applicationName, toolName, true);
		clips.addAll(databaseLink.getBestExamplesOfTool(applicationName, toolName, false));
		
		//XXX REMOVE TEST DATA
		//clips.add(new File("Eclipse21e0149b-f0e7-31ee-85bf-6abb88ddc5c2G"));
		
		JSONArray jarr = new JSONArray();
		for(File clip: clips) {
			JSONArray fileNamesArr = makeFrameList(clip);
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

	public static JSONArray makeFrameList(File clipDir) 
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

}
