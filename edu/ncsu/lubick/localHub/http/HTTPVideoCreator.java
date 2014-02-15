package edu.ncsu.lubick.localHub.http;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

public class HTTPVideoCreator extends TemplateHandlerWithDatabaseLink implements Handler {

	private static final String POST_COMMAND_NTH_USAGE = "nthUsage";
	private static final String POST_COMMAND_PLUGIN_NAME = "pluginName";
	private static final String POST_COMMAND_IS_VIDEO_MADE_FOR_TOOL_USAGE = "isVideoAlreadyMade";
	private static final String POST_COMMAND_MAKE_VIDEO_FOR_TOOL_STREAM = "makeVideo";
	private static final String POST_COMMAND_TOOL_NAME = "toolName";
	private static final String POST_COMMAND_SWAP_MEDIA = "changeToOtherSource";
	private static Logger logger;
	
	private static final File MEDIA_OUTPUT_FOLDER = new File(PostProductionHandler.MEDIA_OUTPUT_FOLDER);
	

	// static initializer
	static
	{
		logger = Logger.getLogger(HTTPVideoCreator.class.getName());
	}

	public HTTPVideoCreator(String matchPattern, WebQueryInterface databaseLink)
	{
		super(matchPattern, databaseLink);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
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
		else if (request.getParameter("thingToDo").equals(POST_COMMAND_SWAP_MEDIA))
		{
			respondToSwapMedia(baseRequest, request, response);
		}
	}

	private void respondToSwapMedia(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String pluginName = request.getParameter(POST_COMMAND_PLUGIN_NAME);
		String toolName = request.getParameter(POST_COMMAND_TOOL_NAME);
		Integer nthUsage = Integer.valueOf(request.getParameter(POST_COMMAND_NTH_USAGE));

		serveUpNthUsageOfMediaIfExists(response, new InternalToolRepresentation(toolName, pluginName, null, nthUsage));
		
		baseRequest.setHandled(true);
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

		try
		{
			this.databaseLink.extractMediaForLastNUsagesOfTool(3, pluginName, toolName);
		}
		catch (MediaEncodingException e)
		{
			respondWithError(baseRequest, response, e);
			return;
		}

		serveUpNthUsageOfMediaIfExists(response, new InternalToolRepresentation(toolName, pluginName, null, 0));
		baseRequest.setHandled(true);
	}

	private void respondWithError(Request baseRequest, HttpServletResponse response, Exception e) throws IOException
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

		serveUpNthUsageOfMediaIfExists(response, new InternalToolRepresentation(toolName, pluginName, null, 0));

		baseRequest.setHandled(true);

	}

	private void serveUpNthUsageOfMediaIfExists(HttpServletResponse response, InternalToolRepresentation itr) throws IOException
	{
		
		String pluginName = itr.humanPluginName;
		String toolName = itr.humanToolName;
		String folderName = PostProductionHandler.makeFileNameStemNoDateForToolPluginMedia(pluginName, toolName);
		
		List<File> mediaFolders = getFoldersPrefixedWith(folderName);
		
		if (itr.nthMostRecent >= mediaFolders.size())
		{
			itr.nthMostRecent = mediaFolders.size() - 1;
		}
		
		if (mediaFolders.size() > 0)
		{
			File nthMediaDir = mediaFolders.get(itr.nthMostRecent);	
			int numFrames = countNumFrames(nthMediaDir);
			List<ToolUsage> toolUsages = databaseLink.getLastNInstancesOfToolUsage(mediaFolders.size(), pluginName, toolName);
					
			itr.directory = nthMediaDir.getName();
			processTemplateWithNameKeysAndNumFrames(response, toolUsages.get(itr.nthMostRecent).getToolKeyPresses(), itr, numFrames, mediaFolders.size());
		}
		else
		{
			logger.debug("No generated media found");

			Map<Object, Object> dataModel = new HashMap<Object, Object>();
			dataModel.put("toolName", toolName);
			dataModel.put("pluginName", pluginName);
			processTemplate(response, dataModel, "videoDoesNotExist.html.piece");
		}
	}

	private List<File> getFoldersPrefixedWith(String folderName)
	{
		File[] outputMediaTypes = MEDIA_OUTPUT_FOLDER.listFiles();
		Arrays.sort(outputMediaTypes, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2)
			{
				return o2.getName().compareTo(o1.getName());		//sort it backwards so that the bigger numbers (most recent dates)
																	//come first
			}
		});
		logger.debug("checking to see if "+Arrays.toString(outputMediaTypes)+" starts with "+folderName);
		List<File> files = new ArrayList<>();
		for(File f: outputMediaTypes)
		{
			if (f.isDirectory() && f.getPath().startsWith(folderName))
			{
				files.add(f);
			}
		}
		return files;
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
	
	private void processTemplateWithNameKeysAndNumFrames(HttpServletResponse response, String keypress, InternalToolRepresentation itr, int numFrames, int totalMediaOptions) throws IOException
	{
		HashMap<Object, Object> templateData = new HashMap<Object, Object>();

		templateData.put("playbackDirectory", itr.directory);
		templateData.put("toolName", itr.humanToolName);
		templateData.put("pluginName", itr.humanPluginName);
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
		otherOptions.add(new DisplayOtherMediaOption(currentlySelected == 0, 0, "Most Recent"));
		if (totalMediaOptions > 1)
		{
			otherOptions.add(new DisplayOtherMediaOption(currentlySelected == 1 ,1, "2nd Most Recent"));
		}
		if (totalMediaOptions > 2)
		{
			otherOptions.add(new DisplayOtherMediaOption(currentlySelected == 2 ,2, "3rd Most Recent"));
		}
		if (totalMediaOptions > 3)
		{
			for (int i = 3;i<totalMediaOptions;i++)
			{
				otherOptions.add(new DisplayOtherMediaOption(currentlySelected == i ,i, ""+(i+1)+"th"));
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
		private String humanPluginName;

		public InternalToolRepresentation(String toolName, String pluginName, String directory, int nthMostRecent)
		{
			this.humanToolName = toolName;
			this.nthMostRecent = nthMostRecent;
			this.directory = directory;
			this.humanPluginName = pluginName;
		}
		
		@Override
		public String toString()
		{
			return "The "+nthMostRecent+"th most recent usage of "+humanToolName;
		}
	}

}
