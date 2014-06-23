package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.ClipOptions;
import edu.ncsu.lubick.localHub.WebQueryInterface;
import edu.ncsu.lubick.localHub.videoPostProduction.PostProductionHandler;

public class HTTPUpdateClip extends TemplateHandlerWithDatabaseLink implements Handler {

	private static Logger logger;

	// static initializer
	static
	{
		logger = Logger.getLogger(HTTPMediaResourceHandler.class.getName());
	}
	
	public HTTPUpdateClip(String matchPattern, WebQueryInterface databaseLink) throws IOException, URISyntaxException
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
			logger.info("I don't know how to handle a GET like this");
		}
		
	}

	private void respondToPost(Request baseRequest, HttpServletRequest request, HttpServletResponse response)
	{		
		String paramStartFrame = request.getParameter("start_frame");
		int startFrame = Integer.parseInt(paramStartFrame == null? "0" : paramStartFrame);
		String paramEndFrame = request.getParameter("end_frame");
		int endFrame = Integer.parseInt(paramEndFrame == null? "0" : paramEndFrame);
		
		String clipId = request.getParameter("clip_id");
		
		databaseLink.updateClipOptions(clipId, new ClipOptions(startFrame, endFrame), true);
		
		response.setStatus(HttpServletResponse.SC_ACCEPTED);
		baseRequest.setHandled(true);
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

}
