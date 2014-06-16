package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HTTPStatusTemplateHandler extends TemplateHandlerWithDatabaseLink
{
	private static final Logger logger = Logger.getLogger(HTTPStatusTemplateHandler.class.getName());
	public static final String TEMPLATE_NAME = "status.html";

	public HTTPStatusTemplateHandler(String matchPattern, WebQueryInterface databaseLink) throws IOException, URISyntaxException
	{
		super(matchPattern, databaseLink);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		logger.info(target);
		if(!strictCheckIfWeHandleThisRequest(target))
		{
			return;
		}
		
		if(baseRequest.getMethod().equals("GET"))
		{
			handleGet(baseRequest, response);
		}
	}
	
	private void handleGet(Request baseRequest, HttpServletResponse response)
	{
		Map<Object, Object> templateData = addThisUserInfoToModel(new HashMap<>());
		
		try {
			processTemplate(response, templateData, TEMPLATE_NAME);
		} catch (IOException e) {
			getLogger().error("Problem processing template", e);
		}
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
