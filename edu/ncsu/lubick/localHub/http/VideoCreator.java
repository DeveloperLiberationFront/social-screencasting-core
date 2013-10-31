package edu.ncsu.lubick.localHub.http;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.WebQueryInterface;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.VideoEncodingException;

public class VideoCreator extends TemplateHandlerWithDatabaseLink implements Handler {

	private static final String POST_COMMAND_PLUGIN_NAME = "pluginName";
	private static final String POST_COMMAND_IS_VIDEO_MADE_FOR_TOOL_USAGE = "isVideoAlreadyMade";
	private static final String POST_COMMAND_MAKE_VIDEO_FOR_TOOL_STREAM = "makeVideo";
	private static final String POST_COMMAND_TOOL_NAME = "toolName";
	private static Logger logger;

	// static initializer
	static
	{
		logger = Logger.getLogger(LookupHandler.class.getName());
	}

	public VideoCreator(String matchPattern, WebQueryInterface databaseLink)
	{
		super(matchPattern, databaseLink);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
	{
		if (!checkIfWeHandleThisRequest(target))
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

		if (request.getParameter("thingToDo").equals(POST_COMMAND_IS_VIDEO_MADE_FOR_TOOL_USAGE))
		{
			respondToDoesVideoExist(baseRequest, request, response);
		}
		else if (request.getParameter("thingToDo").equals(POST_COMMAND_MAKE_VIDEO_FOR_TOOL_STREAM))
		{
			makeVideo(baseRequest, request, response);
		}
	}

	private void makeVideo(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String pluginName = request.getParameter(POST_COMMAND_PLUGIN_NAME);
		String toolName = request.getParameter(POST_COMMAND_TOOL_NAME);

		if (pluginName == null || toolName == null)
		{
			response.getWriter().println("<span>Internal Error. </span>");
			return;
		}

		File madeVideo;
		try
		{
			madeVideo = this.databaseLink.extractVideoForLastUsageOfTool(pluginName, toolName);
		}
		catch (VideoEncodingException e)
		{
			logger.fatal("Error caught when video making requested: ", e);
			response.getWriter().println("<span>Internal Video Creation Error. </span>");
			response.getWriter().println("<div>");
			response.getWriter().print(e.getLocalizedMessage());
			response.getWriter().println("</div>");
			baseRequest.setHandled(true);
			return;
		}

		response.getWriter().println("<span>This video file has been generated as " + madeVideo.getAbsolutePath() + " </span>");
		baseRequest.setHandled(true);
	}

	private void respondToDoesVideoExist(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String pluginName = request.getParameter(POST_COMMAND_PLUGIN_NAME);
		String toolName = request.getParameter(POST_COMMAND_TOOL_NAME);

		String expectedVideoFileName = PostProductionHandler.makeFileNameForToolPluginMedia(pluginName, toolName);
		File expectedVideoFile = new File(expectedVideoFileName);

		logger.debug("If file existed, it would be called " + expectedVideoFile.getAbsolutePath());
		if (expectedVideoFile.exists())
		{
			logger.debug("It exists!");
			// response.getWriter().println("<span>This video file has been generated as "+expectedVideoFile.getAbsolutePath()+"</span>");
			Map<Object, Object> dataModel = new HashMap<Object, Object>();
			dataModel.put("path", expectedVideoFile.getAbsolutePath());
			logger.debug("template model: " + dataModel);
			processTemplate(response, dataModel, "generatedVideo.html.piece");
		}
		else
		{
			logger.debug("It does not");
			// response.getWriter().println("<p>This video file does not exist yet </p>");
			// response.getWriter().println("<div class='requestGeneration' data-tool-name='"+toolName+"' data-plugin-name='"+pluginName+"'> Click here to generate it</div>");

			Map<Object, Object> dataModel = new HashMap<Object, Object>();
			dataModel.put("toolName", toolName);
			dataModel.put("pluginName", pluginName);
			processTemplate(response, dataModel, "videoDoesNotExist.html.piece");
		}
		baseRequest.setHandled(true);

	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
