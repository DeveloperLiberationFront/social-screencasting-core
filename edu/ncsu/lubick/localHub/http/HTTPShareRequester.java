package edu.ncsu.lubick.localHub.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HTTPShareRequester extends TemplateHandlerWithDatabaseLink implements Handler {

	
	private static final Logger logger = Logger.getLogger(HTTPShareRequester.class);
	
	
	public HTTPShareRequester(String matchPattern, WebQueryInterface databaseLink)
	{
		super(matchPattern, databaseLink);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!this.strictCheckIfWeHandleThisRequest(target))
		{
			return;
		}
		logger.debug(request.getParameterMap());
		
		if ("POST".equals(baseRequest.getMethod()))
		{
			handlePost(baseRequest, request);
		}
		else 
		{
			logger.error("I don't know how to handle a "+baseRequest.getMethod());
		}
	}

	private void handlePost(Request baseRequest, HttpServletRequest request)
	{
		baseRequest.setHandled(true);
		String pluginName = request.getParameter(PARAM_PLUGIN_NAME);
		String toolName = request.getParameter(PARAM_TOOL_NAME);
		String owner = request.getParameter("ownerEmail");
		
		if (pluginName == null || toolName == null || owner == null)
		{
			logger.info("pluginName = "+pluginName +", toolName = "+toolName+", owner = "+owner+", so cancelling request");
			return;
		}
		logger.debug(String.format("Passing along clip request for %s/%s from user %s",pluginName, toolName, owner));
		this.databaseLink.requestClipsFromUser(owner, pluginName, toolName);
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
