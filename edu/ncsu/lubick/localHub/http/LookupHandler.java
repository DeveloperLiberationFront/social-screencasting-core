package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.WebQueryInterface;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * This handler will be able to talk to the database.
 * @author KevinLubick
 *
 */
public class LookupHandler extends TemplateHandlerWithDatabaseLink {

	private static final String POST_COMMAND_PLUGIN_NAME = "pluginName";
	private static final String POST_COMMAND_GET_TOOL_USAGE_FOR_PLUGIN = "getToolUsageForPlugin";
	private static Logger logger;
	
	
	//static initializer
	static {
		logger = Logger.getLogger(LookupHandler.class.getName());
		
	}

	public LookupHandler(String matchPattern, WebQueryInterface databaseLink) 
	{
		super(matchPattern,databaseLink);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if (!checkIfWeHandleThisRequest(target))
		{
			return;
		}
		logger.debug(String.format("HTML Request %s, with target %s" , baseRequest.toString(), target));
		String type = baseRequest.getMethod();
		if (type.equals("POST"))
		{
			respondToPost(baseRequest,request,response);
		}
		else {
			respondToGet(baseRequest, response);
		}
		
	}

	//serve up the webpage with the list of tools 
	private void respondToGet(Request baseRequest, HttpServletResponse response) throws IOException 
	{
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);

		Map<Object, Object> root = getPluginsFollowedDataModelFromDatabase();

		processTemplate(response, root, PLUGIN_VIEWER);
	}

	private void respondToPost(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException 
	{

		logger.debug("POST parameters recieved "+request.getParameterMap());
		logger.debug("PluginName: "+request.getParameter(POST_COMMAND_PLUGIN_NAME));
		
		if (request.getParameter("thingToDo").equals(POST_COMMAND_GET_TOOL_USAGE_FOR_PLUGIN))	
		{
			respondToGetToolUsageForPlugin(baseRequest,request,response);
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
		List<ToolUsage> toolUsages = this.databaseLink.getAllToolUsagesForPlugin(pluginName);
		List<ToolNameAndCount> toolsAndCounts = countUpAllToolUsages(toolUsages);
		
		Map<Object, Object> retval = new HashMap<>();
		retval.put("toolsAndCounts", toolsAndCounts);
		retval.put("plugin", pluginName);
		return retval;
	}

	private List<ToolNameAndCount> countUpAllToolUsages(List<ToolUsage> toolUsages) {
		Map<String, Integer> toolCountsMap = new HashMap<>();
		//add the toolusages to the map
		for(ToolUsage tu:toolUsages)
		{
			Integer previousCount = toolCountsMap.get(tu.getToolName());
			if (previousCount == null)
			{
				previousCount = 0;
			}
			toolCountsMap.put(tu.getToolName(), previousCount + 1);
		}
		//convert the map back to a list
		List<ToolNameAndCount> retVal = new ArrayList<>();
		for(String toolName: toolCountsMap.keySet())
		{
			retVal.add(new ToolNameAndCount(toolName, toolCountsMap.get(toolName)));
		}
		//sort, using the internal comparator
		Collections.sort(retVal);
		return retVal;
	}

	private Map<Object, Object> getPluginsFollowedDataModelFromDatabase() {
		
		List<String> pluginNames = this.databaseLink.getNamesOfAllPlugins();
		
		Map<Object, Object> retVal = new HashMap<>();
		retVal.put("pluginNames", pluginNames);
		
		
		return retVal;
	}
	
	/*
	 * implements TemplatehashModel so that in the templates, its fields can be accessed via ${toolNameAndCount.toolCount}
	 */
	private class ToolNameAndCount implements Comparable<ToolNameAndCount>, TemplateHashModel	
	{
		
		private Integer toolCount;
		private String toolName;

		public ToolNameAndCount(String toolName, Integer toolCount) {
			this.toolName = toolName;
			this.toolCount = toolCount;
		}

		@Override
		public int compareTo(ToolNameAndCount o) 
		{	//sort by tool usage, if they are the same, sort by alphabetical order on the tool name
			if (this.toolCount.compareTo(o.toolCount) != 0)
			{
				return o.toolCount.compareTo(this.toolCount);		//reversed so that Collections.sort sorts in descending order
			}
			return o.toolName.compareTo(this.toolName); 			//reversed so that Collections.sort sorts in descending order
		}
		
		@Override
		public int hashCode() {
			return (toolName+toolCount.toString()).hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ToolNameAndCount)
			{
				return this.hashCode() == obj.hashCode();
			}
			return false;
		}

		@Override
		public String toString() 
		{	//for debugging
			return "ToolNameAndCount [toolName=" + toolName + ", toolCount="
					+ toolCount + "]";
		}

		@Override
		public TemplateModel get(String arg0) throws TemplateModelException {
			if (arg0.equals("toolName"))
			{
				return new SimpleScalar(toolName);
			}
			if (arg0.equals("toolCount"))
			{
				return new SimpleNumber(toolCount);
			}
			throw new TemplateModelException("Does not have a "+arg0);
		}

		@Override
		public boolean isEmpty() throws TemplateModelException {
			return false;
		}
		
		

	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

}
