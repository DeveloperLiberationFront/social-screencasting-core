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
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.videoPostProduction.ImagesToVideoOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import edu.ncsu.lubick.localHub.videoPostProduction.VideoEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.gif.ImagesToGifOutput;
import edu.ncsu.lubick.localHub.videoPostProduction.gif.ImagesToMiniGifOutput;

public class VideoCreator extends TemplateHandlerWithDatabaseLink implements Handler {

	private static final String POST_COMMAND_PLUGIN_NAME = "pluginName";
	private static final String POST_COMMAND_IS_VIDEO_MADE_FOR_TOOL_USAGE = "isVideoAlreadyMade";
	private static final String POST_COMMAND_MAKE_VIDEO_FOR_TOOL_STREAM = "makeVideo";
	private static final String POST_COMMAND_TOOL_NAME = "toolName";
	private static Logger logger;

	// static initializer
	static
	{
		logger = Logger.getLogger(VideoCreator.class.getName());
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

		ToolUsage lastToolUsage = null;
		
		try
		{
			lastToolUsage = this.databaseLink.extractMediaForLastUsageOfTool(pluginName, toolName);
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

		//String bigGifRelativeName = getNameForToolFullGif(pluginName, toolName);
		//bigGifRelativeName = bigGifRelativeName.substring(bigGifRelativeName.indexOf('\\'));
		//String miniGifRelativeName = getNameForToolMiniGif(pluginName, toolName);
		//miniGifRelativeName = miniGifRelativeName.substring(miniGifRelativeName.indexOf('\\'));

		//respondWithGifsAndMetadata(response, toolName, bigGifRelativeName, miniGifRelativeName);
		
		String folderName = PostProductionHandler.makeFileNameForToolPluginMedia(pluginName, toolName);
		File mediaDir = new File(folderName);
		if (!mediaDir.exists() || !mediaDir.isDirectory())
		{
			logger.error("problem with media dir "+mediaDir);
			baseRequest.setHandled(true);
			return;
		}
		int numFrames = countNumFrames(mediaDir);
		
		processTemplateWithNameKeysAndNumFrames(response, lastToolUsage.getToolKeyPresses(), toolName, numFrames);
		baseRequest.setHandled(true);
	}

	private void respondToDoesVideoExist(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		/*
		 * String pluginName = request.getParameter(POST_COMMAND_PLUGIN_NAME); String toolName = request.getParameter(POST_COMMAND_TOOL_NAME);
		 * 
		 * String expectedBigGifFileName = getNameForToolFullGif(pluginName, toolName); File expectedBigGifFile = new File(expectedBigGifFileName);
		 * 
		 * String expectedMiniGifFileName = getNameForToolMiniGif(pluginName, toolName); File expectedMiniGifFile = new File(expectedMiniGifFileName);
		 * 
		 * logger.debug("If gif files existed, they would be called " + expectedBigGifFileName + " and " + expectedMiniGifFileName);
		 * 
		 * if (expectedBigGifFile.exists() && expectedMiniGifFile.exists()) { String bigGifRelativeName = expectedBigGifFile.getName(); String
		 * miniGifRelativeName = expectedMiniGifFile.getName(); logger.debug("It exists!"); respondWithGifsAndMetadata(response, toolName, bigGifRelativeName,
		 * miniGifRelativeName); } else { logger.debug("It does not");
		 * 
		 * Map<Object, Object> dataModel = new HashMap<Object, Object>(); dataModel.put("toolName", toolName); dataModel.put("pluginName", pluginName);
		 * processTemplate(response, dataModel, "videoDoesNotExist.html.piece"); }
		 */
		String keypress = "MENU";
		String toolName = "Testing1388763334";
		String folderName = PostProductionHandler.makeFileNameForToolPluginMedia("Testing", "WhomboTool #1");
		File mediaDir = new File(folderName);
		if (!mediaDir.exists() || !mediaDir.isDirectory())
		{
			logger.error("problem with media dir "+mediaDir);
			baseRequest.setHandled(true);
			return;
		}
		int numFrames = countNumFrames(mediaDir);
		processTemplateWithNameKeysAndNumFrames(response, keypress, toolName, numFrames);

		baseRequest.setHandled(true);

	}

	private int countNumFrames(File mediaDir)
	{
		int count = 0;
		String[] fileNames = mediaDir.list();
		for(String fileName:fileNames)
		{
			if (fileName.startsWith("frame"))
			{
				count++;
			}
		}
		return count;
	}

	public void processTemplateWithNameKeysAndNumFrames(HttpServletResponse response, String keypress, String toolName, int numFrames) throws IOException
	{
		HashMap<Object, Object> templateData = new HashMap<Object, Object>();
		
		templateData.put("toolName", toolName);
		templateData.put("keypress", keypress);
		templateData.put("totalFrames", numFrames);
		processTemplate(response, templateData, "playback.html.piece");
	}

	public void respondWithGifsAndMetadata(HttpServletResponse response, String toolName, String bigGifRelativeName, String miniGifRelativeName)
			throws IOException
	{
		Map<Object, Object> dataModel = new HashMap<Object, Object>();
		dataModel.put("toolName", toolName);
		dataModel.put("bigAnimatedGif", bigGifRelativeName);
		dataModel.put("miniAnimatedGif", miniGifRelativeName);
		logger.debug("template model: " + dataModel);
		processTemplate(response, dataModel, "generatedVideo.html.piece");
	}

	public String getNameForToolVideo(String pluginName, String toolName)
	{
		return PostProductionHandler.makeFileNameForToolPluginMedia(pluginName, toolName) + "." + ImagesToVideoOutput.VIDEO_EXTENSION;
	}

	public String getNameForToolFullGif(String pluginName, String toolName)
	{
		return PostProductionHandler.makeFileNameForToolPluginMedia(pluginName, toolName) + "." + ImagesToGifOutput.GIF_EXTENSION;
	}

	public String getNameForToolMiniGif(String pluginName, String toolName)
	{
		return PostProductionHandler.makeFileNameForToolPluginMedia(pluginName, toolName) + "." + ImagesToMiniGifOutput.MINI_GIF_EXTENSION;
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
