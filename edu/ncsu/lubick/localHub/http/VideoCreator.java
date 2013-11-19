package edu.ncsu.lubick.localHub.http;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.WebQueryInterface;
import edu.ncsu.lubick.localHub.videoPostProduction.MediaEncodingException;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

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
			respondToDoesMediaExist(baseRequest, request, response);
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
		catch (MediaEncodingException e)
		{
			respondWithError(baseRequest, response, e);
			return;
		}

		String folderName = PostProductionHandler.makeFileNameStemForToolPluginMedia(pluginName, toolName);
		File mediaDir = new File(folderName);
		if (!mediaDir.exists() || !mediaDir.isDirectory())
		{
			logger.error("problem with media dir " + mediaDir);
			baseRequest.setHandled(true);
			return;
		}
		int numFrames = countNumFrames(mediaDir);
		
		InternalToolRepresentation itr = new InternalToolRepresentation(toolName, 1, mediaDir.getName());

		processTemplateWithNameKeysAndNumFrames(response, lastToolUsage.getToolKeyPresses(), itr, numFrames);
		baseRequest.setHandled(true);
	}

	private void respondWithError(Request baseRequest, HttpServletResponse response, MediaEncodingException e) throws IOException
	{
		logger.fatal("Error caught when video making requested: ", e);
		response.getWriter().println("<span>Internal Video Creation Error. </span>");
		response.getWriter().println("<div>");
		response.getWriter().print(e.getLocalizedMessage());
		response.getWriter().println("</div>");
		baseRequest.setHandled(true);
		return;
	}

	private void respondToDoesMediaExist(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String pluginName = request.getParameter(POST_COMMAND_PLUGIN_NAME);
		String toolName = request.getParameter(POST_COMMAND_TOOL_NAME);

		String folderName = PostProductionHandler.makeFileNameStemForToolPluginMedia(pluginName, toolName);
		File mediaDir = new File(folderName);

		logger.debug("If media folder existed, it would be called " + mediaDir);

		if (mediaDir.exists() && mediaDir.isDirectory())
		{
			if (!mediaDir.exists() || !mediaDir.isDirectory())
			{
				logger.error("problem with media dir " + mediaDir);
				baseRequest.setHandled(true);
				return;
			}
			int numFrames = countNumFrames(mediaDir);
			ToolUsage lastToolUsage = databaseLink.getLastInstanceOfToolUsage(pluginName, toolName);
			
			InternalToolRepresentation itr = new InternalToolRepresentation(toolName, 1, mediaDir.getName());
			
			processTemplateWithNameKeysAndNumFrames(response, lastToolUsage.getToolKeyPresses(), itr, numFrames);
		}
		else if (mediaDir.exists() && !mediaDir.isDirectory())
		{
			respondWithError(baseRequest, response, new MediaEncodingException("mediaDir was not directory: " + mediaDir));
			return;
		}
		else
		{
			logger.debug("It does not");

			Map<Object, Object> dataModel = new HashMap<Object, Object>();
			dataModel.put("toolName", toolName);
			dataModel.put("pluginName", pluginName);
			processTemplate(response, dataModel, "videoDoesNotExist.html.piece");
		}

		baseRequest.setHandled(true);

	}

	private int countNumFrames(File mediaDir)
	{
		int count = 0;
		String[] fileNames = mediaDir.list();
		for (String fileName : fileNames)
		{
			if (fileName.startsWith("frame"))
			{
				count++;
			}
		}
		return count;
	}

	private void processTemplateWithNameKeysAndNumFrames(HttpServletResponse response, String keypress, InternalToolRepresentation itr, int numFrames) throws IOException
	{
		processTemplateWithNameKeysAndNumFrames(response, keypress, itr, numFrames, 1);
	}
	
	private void processTemplateWithNameKeysAndNumFrames(HttpServletResponse response, String keypress, InternalToolRepresentation itr, int numFrames, int totalMediaOptions) throws IOException
	{
		HashMap<Object, Object> templateData = new HashMap<Object, Object>();

		templateData.put("playbackDirectory", itr.directory);
		templateData.put("toolName", itr.humanToolName);
		templateData.put("keypress", keypress);
		templateData.put("totalFrames", numFrames);

		if (totalMediaOptions > 1)	//if we only have 1 option, don't give more options
		{
			List<DisplayOtherMediaOption> otherOptions = makeMoreOptions(itr.nthMostRecent, totalMediaOptions);

			templateData.put("viewMoreOptions", otherOptions);
		}
		
		processTemplate(response, templateData, "playback.html.piece");
	}

	public List<DisplayOtherMediaOption> makeMoreOptions(int currentlySelected, int totalMediaOptions)
	{
		List<DisplayOtherMediaOption> otherOptions = new ArrayList<>();
		otherOptions.add(new DisplayOtherMediaOption(currentlySelected == 1, 1, "Most Recent"));
		if (totalMediaOptions > 2)
		{
			otherOptions.add(new DisplayOtherMediaOption(currentlySelected == 2 ,2, "2nd Most Recent"));
		}
		if (totalMediaOptions > 3)
		{
			otherOptions.add(new DisplayOtherMediaOption(currentlySelected == 3 ,3, "3rd Most Recent"));
		}
		if (totalMediaOptions > 4)
		{
			for (int i = 4;i<totalMediaOptions;i++)
			{
				otherOptions.add(new DisplayOtherMediaOption(currentlySelected == i ,i, ""+i+"th"));
			}
		}
		return otherOptions;
	}
	


	@Override
	protected Logger getLogger()
	{
		return logger;
	}

	private class DisplayOtherMediaOption implements TemplateHashModel
	{
		boolean isActivated;
		int number;
		String text;


		public DisplayOtherMediaOption(boolean isActivated, int number, String text)
		{
			this.isActivated = isActivated;
			this.number = number;
			this.text = text;
		}

		@Override
		public TemplateModel get(String queryItem) throws TemplateModelException
		{
			switch (queryItem)
			{
			case "additionalClass":
				if (isActivated)
				{
					return new SimpleScalar("activated");
				}
				return new SimpleScalar("");
			case "number":
				return new SimpleNumber(number);
			case "text":
				return new SimpleScalar(text);
			}
			return null;
		}

		@Override
		public boolean isEmpty() throws TemplateModelException
		{
			return false;
		}

	}
	
	private class InternalToolRepresentation
	{
		
		private String humanToolName;
		private int nthMostRecent;
		private String directory;

		public InternalToolRepresentation(String toolName, int nthMostRecent, String directory)
		{
			this.humanToolName = toolName;
			this.nthMostRecent = nthMostRecent;
			this.directory = directory;
		}
		
		@Override
		public String toString()
		{
			return "The "+nthMostRecent+"th most recent usage of "+humanToolName;
		}
	}

}
