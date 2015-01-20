package edu.ncsu.lubick.localHub.http;

import static edu.ncsu.lubick.localHub.http.HTTPUtils.*;
import static edu.ncsu.lubick.util.FileUtilities.*;

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

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.WebQueryInterface;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.util.ClipUtils;
import edu.ncsu.lubick.util.FileUtilities;

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
		if(target.startsWith("/mock")) {
			handleMocks(target, response);
			return;
		}
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
		else if ("GET".equals(type))
		{
			handleGet(target, response);
		}else if("PUT".equals(type)){
			handlePUT(target,request,response);
		}
	}
	
	//Mocking API handler
	private void handleMocks(String target, HttpServletResponse response) throws IOException {
		String[] pieces = target.split("/");
		if("mock".equals(pieces[1])) {
			if(pieces.length == 4) {
				if("clips".equals(pieces[2]))
					replyWithJSONMock(response, "images.json");
			}
			if("clips".equals(pieces[2])) {
				replyWithJSONMock(response, "clips.json");
			}
			if("user_tools".equals(pieces[2])) {
				replyWithJSONMock(response, "user_tools.json");
			}
			if("users".equals(pieces[2])) {
				replyWithJSONMock(response, "users.json");
			}
			if("user".equals(pieces[2])) {
				replyWithJSONMock(response, "user.json");
			}
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
		} else if (pieces.length == 3){		//api/user
			handleGetUserInfo(chopOffQueryString(pieces[2]), response);
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
		
		replyWithAllImagesForClip(response, clipId);
		
	}

	/*
	 * Returns all images, animation frames or otherwise, for a given clip
	 */
	private void replyWithAllImagesForClip(HttpServletResponse response, String clipId) throws IOException
	{
		File clipDir = new File("renderedVideos",clipId);
		try
		{
			JSONArray returnArray = new JSONArray();
			File[] images = nonNull(clipDir.listFiles());
			
			for(int i = 0;i< images.length; i++) {
				File file = images[i];

				logger.info(file.getAbsolutePath());
				BufferedImage img = ImageIO.read(file);
				
				String name = file.getName();
				if (!name.endsWith(PostProductionHandler.FULLSCREEN_IMAGE_FORMAT)) {
					ImageIO.write(img, PostProductionHandler.ANIMATION_FORMAT, byteBufferForImage);
					
					//animation frames need to be reported without an ending
					if (name.contains("."))
						name = name.substring(0, name.indexOf('.'));
				}
				else {
					ImageIO.write(img, PostProductionHandler.FULLSCREEN_IMAGE_FORMAT, byteBufferForImage);
				}
				

				byte[] imageAsBytes = byteBufferForImage.toByteArray();
				
				
				JSONObject jobj = new JSONObject();
				jobj.put("name", name);
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
		
//		//XXX sample data
//		clips.add(new File("renderedVideos/Eclipse16141cfc-87cb-32dc-bc30-fedcad3b7598G"));
//		clips.add(new File("renderedVideos/Eclipse1a46c017-a154-323b-824f-caa732caa84aG"));
		
		JSONArray jarr = new JSONArray();
		for(File clip: clips) {
			JSONArray fileNamesArr = ClipUtils.makeFrameListForClip(clip);
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
				clipObject.put("user", userManager.getUserEmail());		
				clipObject.put("thumbnail", makeThumbnail(clip.getName(), fileNamesArr));
				
				JSONArray eventFrames = new JSONArray().put(25);
				clipObject.put("event_frames", eventFrames);
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

	private JSONObject makeRecordingObj() throws JSONException {
		JSONObject status = new JSONObject();
		status.put("id", "recording");
		status.put("status", LocalHub.isRunning());
		return status;
	}
		
	private String makeThumbnail(String clipId, JSONArray fileNamesArr)
	{
		try {
			int indexForThumbnail = fileNamesArr.length() > 25? 25 : fileNamesArr.length()/2;

			File file = new File("renderedVideos/"+clipId, fileNamesArr.getString(indexForThumbnail));

			return ClipUtils.makeBase64EncodedThumbnail(file);
		}
		catch (IOException | JSONException e)
		{
			logger.error("Problem making a thumbnail " + clipId + " " + fileNamesArr);
			return null;
		}
	}
	
	//Get a mock file content. Mock files are stored in [project dir]/mocks folder. 
	private void replyWithJSONMock(HttpServletResponse response, String filename) throws IOException {
		response.setContentType("application/json");
		response.setStatus(200);
		File mock = new File("mocks/" + filename);
		response.getWriter().write(FileUtilities.readAllFromFile(mock));
		response.getWriter().close();
	}

}
