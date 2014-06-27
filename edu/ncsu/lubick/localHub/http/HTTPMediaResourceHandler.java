package edu.ncsu.lubick.localHub.http;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.WebQueryInterface;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class HTTPMediaResourceHandler extends AbstractHandler implements Handler {

	private static final String PARAM_CLIP_NAME = "clipName";
	
	private static final String PERFORM_ACTION = "thingToDo";
	private static final String QUERY_CLIP_EXISTANCE = "queryClipExistance";
	private static final String GET_IMAGES_FOR_CLIP = "getImages";
	


	private static Logger logger;
	static
	{
		logger = Logger.getLogger(HTTPMediaResourceHandler.class.getName());
	}

	private WebQueryInterface databaseLink;

	public HTTPMediaResourceHandler(WebQueryInterface databaseLink) throws IOException, URISyntaxException
	{
		this.databaseLink = databaseLink;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!target.startsWith("/mediaServer")) {
			return;
		}
		logger.debug(String.format("HTML Request %s, with target %s", baseRequest.toString(), target));
		baseRequest.setHandled(true);
		response.setContentType("application/json");
		String type = baseRequest.getMethod();

		if ("GET".equals(type))
		{
			respondToGet(target, request, response);
		}
		else
		{
			logger.info("I don't know how to handle a POST like this " +target);
		}

	}

	private void respondToGet(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
//		String[] pieces = target.split("/");
//		/*
//		 * /mediaServer/:creator/:app/:tool/:name/'
//		 */
//		logger.info("Broken up target "+pieces);
//		if (pieces.length < 2) {
//			
//		}
	}

	private void respondToGetClipImages(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String clipName = request.getParameter(PARAM_CLIP_NAME);

		File clipDir = new File(PostProductionHandler.MEDIA_OUTPUT_FOLDER,clipName);

		JSONObject clipObject = new JSONObject();
		JSONArray fileNamesArr = new JSONArray();


		if (clipDir.exists() && clipDir.isDirectory())
		{
			String[] files = clipDir.list();
			Arrays.sort(files);
			for(String imageFile: files)
			{
				fileNamesArr.put(imageFile);
			}
		}
		else {
			logger.info("Clip "+clipName+" does not exist");
		}


		try{
			JSONObject fileNamesObject = new JSONObject();
			fileNamesObject.put("filenames", fileNamesArr);
			fileNamesObject.put("name", clipName);
			clipObject.put("clip", fileNamesObject);

			response.setContentType("application/json");
			
			logger.debug("Returning clip info "+clipName);
			
			clipObject.write(response.getWriter());
		}
		catch (JSONException e)
		{
			logger.error("Problem compiling clip names and writing them out",e);
		}

		baseRequest.setHandled(true);


	}


	private void respondToDoesMediaExist(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
//		String pluginName = request.getParameter(PARAM_PLUGIN_NAME);
//		String toolName = request.getParameter(PARAM_TOOL_NAME);
//
//		List<File> keyClips = databaseLink.getBestExamplesOfTool(pluginName, toolName, true);
//
//		List<File> guiClips = databaseLink.getBestExamplesOfTool(pluginName, toolName, false);
//		
//		JSONArray keyJarr = new JSONArray();
//		for(File f: keyClips)
//		{
//			keyJarr.put(f.getName());
//		}
//		
//		JSONArray guiJarr = new JSONArray();
//		for(File f: guiClips)
//		{
//			guiJarr.put(f.getName());
//		}
//		JSONObject clips = new JSONObject();
//
//		try
//		{
//			// Testing data
//			keyJarr.put("Eclipsefc7fb775-e185-31af-83cb-1d315a809952K");
//			keyJarr.put("Eclipsef24fe0e7-d0c1-36c7-a4d0-565e131f35ecK");
//			guiJarr.put("Eclipse13d5a993-e46f-3b7f-862a-bfefa5831901G");
//			guiJarr.put("Eclipse1a46c017-a154-323b-824f-caa732caa84aG");
//			guiJarr.put("Eclipse29bf2b83-2e3d-3855-9286-ee7f69db64c1G");
//
//			
//			clips.put("keyclips",keyJarr);
//			clips.put("guiclips",guiJarr);
//
//			response.setContentType("application/json");
//			clips.write(response.getWriter());
//		}
//		catch (JSONException e)
//		{
//			logger.error("Problem compiling clip names and writing them out",e);
//		}
//
//
//		baseRequest.setHandled(true);

	}




}
