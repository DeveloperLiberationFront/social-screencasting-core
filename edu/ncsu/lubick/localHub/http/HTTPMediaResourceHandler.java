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
		ToolUsage clip = databaseLink.getToolUsageByFolder(PostProductionHandler.MEDIA_OUTPUT_FOLDER + clipName);
		
		if (clip == null) {
			response.sendError(500, "Corrupted database");
			baseRequest.setHandled(true);
			return;
		}

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
			fileNamesObject.put("start_data", clip.getStartData());
			fileNamesObject.put("end_data", clip.getEndData());
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
//			//keyJarr.put("Eclipsef100758a-fc1f-3cdb-a1c0-7287f184d10d");
//			//keyJarr.put("Eclipsee667cfd3-0bd8-3af8-93d7-10d16ab2f854");
//			//guiJarr.put("Eclipsee434f382-7183-3cc5-8380-2137816a48d4");
//			//guiJarr.put("Eclipse47397aaf-c70f-3aa1-9df5-a87f5a583af3");
//			//guiJarr.put("Eclipse06ac5c3c-da64-3300-9a74-6fed83aa2722");
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
