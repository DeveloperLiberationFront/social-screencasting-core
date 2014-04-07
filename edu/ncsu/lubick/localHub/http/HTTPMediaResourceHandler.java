package edu.ncsu.lubick.localHub.http;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HTTPMediaResourceHandler extends TemplateHandlerWithDatabaseLink implements Handler {

	private static final String PARAM_CLIP_NAME = "clipName";
	
	private static final String PERFORM_ACTION = "thingToDo";
	private static final String QUERY_CLIP_EXISTANCE = "queryClipExistance";
	private static final String GET_IMAGES_FOR_CLIP = "getImages";
	


	private static Logger logger;

	// static initializer
	static
	{
		logger = Logger.getLogger(HTTPMediaResourceHandler.class.getName());
	}

	public HTTPMediaResourceHandler(String matchPattern, WebQueryInterface databaseLink)
	{
		super(matchPattern, databaseLink);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!strictCheckIfWeHandleThisRequest(target))
		{
			return;
		}
		logger.debug(String.format("HTML Request %s, with target %s", baseRequest.toString(), target));
		String type = baseRequest.getMethod();

		if ("POST".equals(type))
		{
			respondToPost(baseRequest, request, response);
		}
		else
		{
			logger.info("I don't know how to handle a GET like this");
		}

	}

	private void respondToPost(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		logger.debug("POST parameters recieved " + request.getParameterMap());
		logger.debug("PluginName: " + request.getParameter(PARAM_PLUGIN_NAME));

		if (QUERY_CLIP_EXISTANCE.equals(request.getParameter(PERFORM_ACTION)))
		{
			respondToDoesMediaExist(baseRequest, request, response);
		}
		else if (GET_IMAGES_FOR_CLIP.equals(request.getParameter(PERFORM_ACTION))) 
		{
			respondToGetClipImages(baseRequest, request, response);
		}
		else {
			logger.error("Cannot handle "+request.getParameter(PERFORM_ACTION));
			baseRequest.setHandled(true);
		}
	}

	private void respondToGetClipImages(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String clipName = request.getParameter(PARAM_CLIP_NAME);

		File clipDir = new File("renderedVideos/",clipName);

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
		String pluginName = request.getParameter(PARAM_PLUGIN_NAME);
		String toolName = request.getParameter(PARAM_TOOL_NAME);

		List<File> keyClips = databaseLink.getBestExamplesOfTool(pluginName, toolName, true);

		List<File> guiClips = databaseLink.getBestExamplesOfTool(pluginName, toolName, false);
		
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
		JSONObject clips = new JSONObject();

		try
		{
			// Testing data
			keyJarr.put("Eclipsef100758a-fc1f-3cdb-a1c0-7287f184d10d");
			keyJarr.put("Eclipsee667cfd3-0bd8-3af8-93d7-10d16ab2f854");
			//guiJarr.put("Eclipsee434f382-7183-3cc5-8380-2137816a48d4");
			//guiJarr.put("Eclipse47397aaf-c70f-3aa1-9df5-a87f5a583af3");
			//guiJarr.put("Eclipse06ac5c3c-da64-3300-9a74-6fed83aa2722");

			
			clips.put("keyclips",keyJarr);
			clips.put("guiclips",guiJarr);

			response.setContentType("application/json");
			clips.write(response.getWriter());
		}
		catch (JSONException e)
		{
			logger.error("Problem compiling clip names and writing them out",e);
		}


		baseRequest.setHandled(true);

	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}




}
