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

	private static final String CLIP_NAME = "clipName";
	private static final String PERFORM_ACTION = "thingToDo";

	private static final String POST_COMMAND_PLUGIN_NAME = "pluginName";

	private static final String POST_COMMAND_TOOL_NAME = "toolName";

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

		if (type.equals("POST"))
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
		logger.debug("PluginName: " + request.getParameter(POST_COMMAND_PLUGIN_NAME));

		if (request.getParameter(PERFORM_ACTION).equals(QUERY_CLIP_EXISTANCE))
		{
			respondToDoesMediaExist(baseRequest, request, response);
		}
		else if (request.getParameter(PERFORM_ACTION).equals(GET_IMAGES_FOR_CLIP))
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
		String clipName = request.getParameter(CLIP_NAME);

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


		try{
			JSONObject fileNamesObject = new JSONObject();
			fileNamesObject.put("filenames", fileNamesArr);
			fileNamesObject.put("name", clipName);
			clipObject.put("clip", fileNamesObject);

			response.setContentType("application/json");
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
		String pluginName = request.getParameter(POST_COMMAND_PLUGIN_NAME);
		String toolName = request.getParameter(POST_COMMAND_TOOL_NAME);

		List<File> browserPackages = databaseLink.getBestExamplesOfTool(pluginName, toolName);

		JSONArray jarr = new JSONArray();
		for(File f: browserPackages)
		{
			jarr.put(f.getName());
		}
		JSONObject clips = new JSONObject();

		try
		{
			jarr.put("Testingfdcb42bc-b6c2-3884-ad3a-4179e19e771d");
			jarr.put("Testing4115af06-d507-3e79-96ba-9d2c6689ad9b");
			clips.put("clips",jarr);

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
