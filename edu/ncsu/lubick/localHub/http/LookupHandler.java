package edu.ncsu.lubick.localHub.http;

import java.io.File;
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
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.ncsu.lubick.localHub.LocalHub;
import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

/**
 * This handler will be able to talk to the database.
 * @author KevinLubick
 *
 */
public class LookupHandler extends AbstractHandler {

	private static final String POST_COMMAND_PLUGIN_NAME = "pluginName";
	private static final String POST_COMMAND_GET_TOOL_USAGE_FOR_PLUGIN = "getToolUsageForPlugin";
	private static final String PLUGIN_VIEWER = "index.html";
	private static final String DISPLAY_TOOL_USAGE = "displayToolUsage.html";
	private static Logger logger;
	private static Configuration cfg;
	
	private String httpRequestPattern;
	private LocalHub databaseLink;	
	
	//static initializer
	static {
		logger = Logger.getLogger(LookupHandler.class.getName());
		try {
			logger.trace("Setting up template configuration");
			setupTemplateConfiguration();
		} catch (IOException e) {
			logger.fatal("There was a problem booting up the template configuration");
		}
	}

	public LookupHandler(String matchPattern, LocalHub localHub) 
	{
		this.httpRequestPattern = matchPattern;
		this.databaseLink = localHub;
	}

	private static void setupTemplateConfiguration() throws IOException {
		cfg = new Configuration();

		cfg.setDirectoryForTemplateLoading(new File("./frontend/templates"));

		// Specify how templates will see the data-model. This is an advanced topic...
		// for now just use this:
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		cfg.setDefaultEncoding("UTF-8");

		// Sets how errors will appear. Here we assume we are developing HTML pages.
		// For production systems TemplateExceptionHandler.RETHROW_HANDLER is better.
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);

		cfg.setIncompatibleImprovements(new Version(2, 3, 20));  // FreeMarker 2.3.20 
	}
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if (!target.equals(httpRequestPattern))
		{
			logger.debug("LookupHandler passing on target "+target);
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

	private void processTemplate(HttpServletResponse response, Map<Object, Object> templateData, String templateName) throws IOException {
		Template temp = cfg.getTemplate(templateName);

		try {
			temp.process(templateData, response.getWriter());
		} catch (TemplateException e) {
			logger.error("Problem with the template",e);
		}
	}

	private void respondToPost(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException 
	{

		logger.debug("POST parameters recieved "+request.getParameterMap());
		//logger.debug("Thing to do: "+request.getParameter("thingToDo"));
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

}
