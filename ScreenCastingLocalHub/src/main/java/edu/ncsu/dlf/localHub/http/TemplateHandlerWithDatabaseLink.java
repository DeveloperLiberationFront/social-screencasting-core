package edu.ncsu.dlf.localHub.http;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.ncsu.dlf.localHub.UserManager;
import edu.ncsu.dlf.localHub.WebQueryInterface;

public abstract class TemplateHandlerWithDatabaseLink extends AbstractHandler {

	protected static final String DISPLAY_TOOL_USAGE = "index.html";
	protected static final String PARAM_PLUGIN_NAME = "pluginName";
	protected static final String PARAM_TOOL_NAME = "toolName";
	protected String httpRequestPattern;
	protected WebQueryInterface databaseLink;

	protected UserManager userManager = HTTPServer.getUserManager();

	public TemplateHandlerWithDatabaseLink(String matchPattern, WebQueryInterface databaseLink) throws IOException, URISyntaxException
	{
		this.httpRequestPattern = matchPattern;
		this.databaseLink = databaseLink;
	}

	protected boolean strictCheckIfWeHandleThisRequest(String target)
	{
		getLogger().trace("Does "+target+" equal "+httpRequestPattern);
		if (!httpRequestPattern.equals(target))
		{
			getLogger().trace("passing on target " + target);
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
