package edu.ncsu.dlf.localHub.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class VersionHandler extends AbstractHandler {
	private String expectedTarget;
	
	private static Logger logger;

	// static initializer
	static
	{
		logger = Logger.getLogger(VersionHandler.class.getName());
	}

	public VersionHandler(String expectedTarget) {
		this.expectedTarget = expectedTarget;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (target.equals(expectedTarget))
		{
			logger.debug(String.format("HTML Request %s, with target %s", baseRequest.toString(), target));
			respond(baseRequest, response);
		}
	}
	
	// serve up the webpage with the list of tools
	private void respond(Request baseRequest, HttpServletResponse response) throws IOException
	{
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);

		response.getWriter().append(HTTPServer.VERSION_ID);
	}

}
