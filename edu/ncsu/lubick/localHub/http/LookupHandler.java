package edu.ncsu.lubick.localHub.http;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/**
 * This handler will be able to talk to the database.
 * @author KevinLubick
 *
 */
public class LookupHandler extends AbstractHandler {

	private static final String PLUGIN_VIEWER = "index.html";
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

		// Specify the data source where the template files come from. Here I set a
		// plain directory for it, but non-file-system are possible too:
		cfg.setDirectoryForTemplateLoading(new File("./frontend/templates"));

		// Specify how templates will see the data-model. This is an advanced topic...
		// for now just use this:
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		cfg.setDefaultEncoding("UTF-8");

		// Sets how errors will appear. Here we assume we are developing HTML pages.
		// For production systems TemplateExceptionHandler.RETHROW_HANDLER is better.
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);

		// At least in new projects, specify that you want the fixes that aren't
		// 100% backward compatible too (these are very low-risk changes as far as the
		// 1st and 2nd version number remains):
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));  // FreeMarker 2.3.20 
	}
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if (!target.equals(httpRequestPattern))
		{
			logger.debug("LookupHandler passing on target "+target);
			return;
		}
		logger.debug("Request was a "+baseRequest.getMethod());
		String type = baseRequest.getMethod();
		if (type.equals("POST"))
		{
			respondToPost(target,baseRequest,request,response);
		}
		else {
			respondToGet(target, baseRequest, request, response);
		}
		
	}

	//serve up the webpage with the list of tools 
	private void respondToGet(String target, Request baseRequest, HttpServletResponse response) throws IOException 
	{
		logger.debug(String.format("HTML Request %s, with target %s" , baseRequest.toString(), target));
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);

		Map<Object, Object> root = getPluginsFollowedDataModelFromDatabase();

		Template temp = cfg.getTemplate(PLUGIN_VIEWER);

		try {
			temp.process(root, response.getWriter());
		} catch (TemplateException e) {
			logger.error("Problem with the template",e);
		}
	}

	private void respondToPost(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) 
	{
		// TODO Auto-generated method stub
		http://www.echoecho.com/htmlforms07.htm
			
		
	}

	private Map<Object, Object> getPluginsFollowedDataModelFromDatabase() {
		
		List<String> pluginNames = this.databaseLink.getNamesOfAllPlugins();
		
		Map<Object, Object> retVal = new HashMap<>();
		retVal.put("pluginNames", pluginNames);
		
		
		return retVal;
	}

}
