package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.WebQueryInterface;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public abstract class TemplateHandlerWithDatabaseLink extends AbstractHandler {

	private static final String TEMPLATES_DIR = "/templates";
	protected static final String PLUGIN_VIEWER = "index.html";
	protected static final String DISPLAY_TOOL_USAGE = "displayToolUsage.html";
	protected static final String PARAM_PLUGIN_NAME = "pluginName";
	protected static final String PARAM_TOOL_NAME = "toolName";
	protected String httpRequestPattern;
	protected WebQueryInterface databaseLink;
	protected static Configuration cfg; // Template Configuration
	protected UserManager userManager = HTTPServer.getUserManager();

	public TemplateHandlerWithDatabaseLink(String matchPattern, WebQueryInterface databaseLink)
	{
		this.httpRequestPattern = matchPattern;
		this.databaseLink = databaseLink;
		try
		{
			getLogger().trace("Setting up template configuration");
			setupTemplateConfiguration();
		}
		catch (IOException | URISyntaxException e)
		{
			getLogger().fatal("There was a problem booting up the template configuration");
		}
	}

	private static void setupTemplateConfiguration() throws IOException, URISyntaxException
	{
		cfg = new Configuration();

		TemplateLoader loader = new ClassTemplateLoader(TemplateHandlerWithDatabaseLink.class, TEMPLATES_DIR);
		cfg.setTemplateLoader(loader);

		// Specify how templates will see the data-model. This is an advanced
		// topic...
		// for now just use this:
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		cfg.setDefaultEncoding("UTF-8");

		// Sets how errors will appear. Here we assume we are developing HTML
		// pages.
		// For production systems TemplateExceptionHandler.RETHROW_HANDLER is
		// better.
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);

		cfg.setIncompatibleImprovements(new Version(2, 3, 20)); // FreeMarker
																// 2.3.20
	}

	protected void processTemplate(HttpServletResponse response, Map<Object, Object> templateData, String templateName) throws IOException
	{
		Template temp = cfg.getTemplate(templateName);

		try
		{
			temp.process(templateData, response.getWriter());
		}
		catch (TemplateException e)
		{
			getLogger().error("Problem with the template", e);
		}
	}

	protected boolean strictCheckIfWeHandleThisRequest(String target)
	{
		getLogger().debug("Does "+target+" equal "+httpRequestPattern);
		if (!httpRequestPattern.equals(target))
		{
			getLogger().debug("passing on target " + target);
			return false;
		}
		return true;
	}

	protected abstract Logger getLogger();

	protected Map<Object, Object> addThisUserInfoToModel(Map<Object, Object> model)
	{
		model.put("userName", userManager.getUserName());
		model.put("userEmail", userManager.getUserEmail());
		model.put("userToken", userManager.getUserToken());
		return model;
	}

}
