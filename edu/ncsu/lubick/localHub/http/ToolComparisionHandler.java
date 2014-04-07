package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.WebQueryInterface;
import edu.ncsu.lubick.util.ToolCountStruct;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * This handler will be able to talk to the database.
 * 
 * @author KevinLubick
 * 
 */
public class ToolComparisionHandler extends TemplateHandlerWithDatabaseLink {

	private static final String POST_COMMAND_PLUGIN_NAME = "pluginName";
	private static final String POST_COMMAND_GET_TOOL_USAGE_FOR_PLUGIN = "getToolUsageForPlugin";
	
	private static final Logger logger = Logger.getLogger(ToolComparisionHandler.class);

	public ToolComparisionHandler(String matchPattern, WebQueryInterface databaseLink)
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
			respondToGet(baseRequest, response);
		}

	}

	// serve up the webpage with the list of tools
	private void respondToGet(Request baseRequest, HttpServletResponse response) throws IOException
	{
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		
		List<String> pluginNames = this.databaseLink.getNamesOfAllPlugins();
		
		String pluginName = baseRequest.getParameter("pluginName");
		if (pluginName == null) {
			pluginName = pluginNames.get(0);
		}
		Map<Object,Object> dataModel = getToolUsagesAndCountsFromDatabase(pluginName);
		
		processTemplate(response, dataModel, DISPLAY_TOOL_USAGE);
	}

	private void respondToPost(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		logger.debug("POST parameters recieved " + request.getParameterMap());
		logger.debug("PluginName: " + request.getParameter(POST_COMMAND_PLUGIN_NAME));

		if (POST_COMMAND_GET_TOOL_USAGE_FOR_PLUGIN.equals(request.getParameter("thingToDo")))
		{
			respondToGetToolUsageForPlugin(baseRequest, request, response);
		}

	}

	private void respondToGetToolUsageForPlugin(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		Map<Object, Object> dataModel = getToolUsagesAndCountsFromDatabase(request.getParameter(POST_COMMAND_PLUGIN_NAME));
		processTemplate(response, dataModel, DISPLAY_TOOL_USAGE);

		baseRequest.setHandled(true);
	}

	private Map<Object, Object> getToolUsagesAndCountsFromDatabase(String pluginName)
	{
		List<ToolCountStruct> toolUsages = this.databaseLink.getAllToolAggregateForPlugin(pluginName);
		List<ToolCountTemplateModel> toolsAndCounts = convertToTemplateModels(toolUsages);
		List<String> pluginNames = this.databaseLink.getNamesOfAllPlugins();
		
		Map<Object, Object> retval = new HashMap<>();
		retval.put("myToolsAndCounts", toolsAndCounts);
		retval.put("plugin", pluginName);
		retval.put("pluginNames", pluginNames);
		addThisUserInfoToModel(retval); 	
		return retval; 
	}

	private List<ToolCountTemplateModel> convertToTemplateModels(List<ToolCountStruct> toolcounts)
	{
		List<ToolCountTemplateModel> retVal = new ArrayList<>();
		
		for(ToolCountStruct count:toolcounts) 
		{
			retVal.add(new ToolCountTemplateModel(count));
		}
		
		return retVal;
	}

//	@Deprecated
//	private List<ToolCountTemplateModel> countUpAllToolUsages(List<ToolUsage> toolUsages)
//	{
//		//TODO this is duplicated code.  Perhaps refactor
//		Map<String, Integer> toolCountsMap = new HashMap<>();
//		// add the toolusages to the map
//		for (ToolUsage tu : toolUsages)
//		{
//			Integer previousCount = toolCountsMap.get(tu.getToolName());
//			if (previousCount == null)
//			{
//				previousCount = 0;
//			}
//			toolCountsMap.put(tu.getToolName(), previousCount + 1);
//		}
//		// convert the map back to a list
//		List<ToolCountTemplateModel> retVal = new ArrayList<>();
//		for (String toolName : toolCountsMap.keySet())
//		{
//			retVal.add(new ToolCountTemplateModel(toolName, toolCountsMap.get(toolName)));
//		}
//		// sort, using the internal comparator
//		Collections.sort(retVal);
//		return retVal;
//	}

	/*
	 * implements TemplatehashModel so that in the templates, its fields can be accessed via ${toolNameAndCount.toolCount}
	 * implements Comparable so that I can sort them on the server before sending them out
	 */
	private class ToolCountTemplateModel extends ToolCountStruct implements TemplateHashModel
	{
		public ToolCountTemplateModel(String toolName, int guiToolCount, int keyboardCount)
		{
			super(toolName.replace("'","&#39").replace("\"", "&#34"), guiToolCount, keyboardCount);
		}


		public ToolCountTemplateModel(ToolCountStruct count)
		{
			this(count.toolName,count.guiToolCount, count.keyboardCount);
		}


		@Override
		public String toString()
		{ // for debugging
			return "ToolCountTemplateModel [toolName=" + toolName + ", toolCount="
					+ guiToolCount + "]";
		}

		@Override
		public TemplateModel get(String arg0) throws TemplateModelException
		{
			if ("toolName".equals(arg0))
			{
				return new SimpleScalar(toolName);
			}
			if ("toolCount".equals(arg0))
			{
				return new SimpleNumber(guiToolCount);
			}
			throw new TemplateModelException("Does not have a " + arg0);
		}

		@Override
		public boolean isEmpty() throws TemplateModelException
		{
			return false;
		}

	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
