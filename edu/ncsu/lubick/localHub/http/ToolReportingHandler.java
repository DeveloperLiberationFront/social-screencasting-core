package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	
	private String httpRequestPattern;
	private WebToolReportingInterface toolReportingInterface;
	
	private static Logger logger = Logger.getLogger(ToolReportingHandler.class.getName());
	
	ExecutorService threadPool = Executors.newFixedThreadPool(1);

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
		logger.info("Finished handling tools");
		response.setStatus(HttpServletResponse.SC_ACCEPTED);
		baseRequest.setHandled(true);
	}

	private void respondToPost(HttpServletRequest request)
	{
		logger.debug("POST parameters received " + request.getParameterMap());
		String pluginName = request.getParameter(POST_PROPERTY_PLUGIN_NAME);
		String jsonArrayOfToolUsage = request.getParameter(POST_PROPERTY_JSON_ARRAY_TOOL_USAGES);
		logger.debug(""+ pluginName+ " " +jsonArrayOfToolUsage);
		
		if (pluginName == null || jsonArrayOfToolUsage == null) {
			logger.info("Bad parameters... not reporting");
			return;
		}
		
		ToolStream ts = ToolStream.generateFromJSON(jsonArrayOfToolUsage);
		ts.setAssociatedPlugin(pluginName);
		
		
		asyncReportToolStream(ts);
		
	}

	private void asyncReportToolStream(final ToolStream ts)
	{
		threadPool.execute(new Runnable() {
			
			@Override
			public void run()
			{
				toolReportingInterface.reportToolStream(ts);
			}
		});		
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
