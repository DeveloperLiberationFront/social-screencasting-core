package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.ncsu.lubick.localHub.ToolStream;

public class ToolReportingHandler extends AbstractHandler  {

	private static final String POST_PROPERTY_PLUGIN_NAME = "pluginName";
	private static final String POST_PROPERTY_JSON_ARRAY_TOOL_USAGES = "toolUsages";
	private static final String POST_PROPERTY_TOOLSTREAM_TIME = "toolStreamTime";
	
	private String httpRequestPattern;
	private WebToolReportingInterface toolReportingInterface;
	
	private static Logger logger = Logger.getLogger(ToolReportingHandler.class.getName());

	public ToolReportingHandler(String location, WebToolReportingInterface toolReportingInterface)
	{
		this.httpRequestPattern = location;
		this.toolReportingInterface = toolReportingInterface;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!checkIfWeHandleThisRequest(target))
		{
			return;
		}
		logger.debug(String.format("HTML Request %s, with target %s", baseRequest.toString(), target));
		String type = baseRequest.getMethod();
		
		if (type.equals("POST"))
		{
			respondToPost(request);
		}
		else
		{
			logger.info("I don't know how to handle a GET like this");
		}
		response.setStatus(HttpServletResponse.SC_ACCEPTED);
		baseRequest.setHandled(true);
	}

	private void respondToPost(HttpServletRequest request)
	{
		logger.debug("POST parameters recieved " + request.getParameterMap());
		String pluginName = request.getParameter(POST_PROPERTY_PLUGIN_NAME);
		String jsonArrayOfToolUsage = request.getParameter(POST_PROPERTY_JSON_ARRAY_TOOL_USAGES);
		logger.debug(""+ pluginName+ " " +jsonArrayOfToolUsage.toString());
		
		//Long timeStamp = Long.decode(request.getParameter(POST_PROPERTY_TOOLSTREAM_TIME));
		ToolStream ts = ToolStream.generateFromJSON(jsonArrayOfToolUsage);
		ts.setAssociatedPlugin(pluginName);
		//ts.setTimeStamp(new Date(timeStamp));
		toolReportingInterface.reportToolStream(ts);
	}

	private boolean checkIfWeHandleThisRequest(String target)
	{
		if (!target.equals(httpRequestPattern))
		{
			logger.debug("LookupHandler passing on target " + target);
			return false;
		}
		return true;
	}
	

}
