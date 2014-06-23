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
import org.eclipse.jetty.util.component.LifeCycle;

import edu.ncsu.lubick.localHub.ToolStream;

public class HTTPToolReportingHandler extends AbstractHandler  {

	private static final String POST_PROPERTY_PLUGIN_NAME = "pluginName";
	private static final String POST_PROPERTY_JSON_ARRAY_TOOL_USAGES = "toolUsages";
	
	private static final String ENDPOINT_REPORT_TOOL = "reportTool";
	private static final String ENDPOINT_UPDATE_ACTIVITY = "updateActivity";
	
	private WebToolReportingInterface toolReportingInterface;
	
	private static Logger logger = Logger.getLogger(HTTPToolReportingHandler.class.getName());
	
	private ExecutorService threadPool = Executors.newFixedThreadPool(1);

	public HTTPToolReportingHandler(WebToolReportingInterface toolReportingInterface)
	{
		this.toolReportingInterface = toolReportingInterface;
		
		prepareShutdownListener();
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
		
		if ("POST".equals(type)) 
		{
			if (ENDPOINT_REPORT_TOOL.equals(target)) {
				respondToReportingPost(request);
			} else if (ENDPOINT_UPDATE_ACTIVITY.equals(target))
			{
				respondToUpdateActivity(request);
			}
			else {
				logger.fatal("What?  I thought we handled " +target);
			}
		}
		else
		{
			logger.info("I don't know how to handle a GET like this");
		}
		logger.info("Finished handling reporting");
		response.setStatus(HttpServletResponse.SC_ACCEPTED);
		baseRequest.setHandled(true);
	}

	private void respondToUpdateActivity(HttpServletRequest request)
	{
		logger.debug("POST parameters received " + request.getParameterMap());
		String pluginName = request.getParameter(POST_PROPERTY_PLUGIN_NAME);
		boolean isActive = Boolean.parseBoolean(request.getParameter(POST_PROPERTY_PLUGIN_NAME));
		
		this.toolReportingInterface.updateActivity(pluginName, isActive);
	}

	private void respondToReportingPost(HttpServletRequest request)
	{
		logger.debug("POST parameters received " + request.getParameterMap());
		String pluginName = request.getParameter(POST_PROPERTY_PLUGIN_NAME);
		String jsonArrayOfToolUsage = request.getParameter(POST_PROPERTY_JSON_ARRAY_TOOL_USAGES);
		logger.debug(""+ pluginName+ " " +jsonArrayOfToolUsage);
		
		if (pluginName == null || jsonArrayOfToolUsage == null) {
			logger.info("Bad parameters... not reporting");
			return;
		}
		logger.info(jsonArrayOfToolUsage);
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
		if (ENDPOINT_REPORT_TOOL.equals(target) || ENDPOINT_UPDATE_ACTIVITY.equals(target))
		{
			return true;
		}
		logger.trace("LookupHandler passing on target " + target);
		return false;
	}

	private void prepareShutdownListener()
	{
		addLifeCycleListener(new Listener() {
			
			@Override
			public void lifeCycleStopping(LifeCycle event)
			{
				threadPool.shutdown();
			}
			
			@Override
			public void lifeCycleStopped(LifeCycle event)
			{//nothing
			}
			
			@Override
			public void lifeCycleStarting(LifeCycle event)
			{//nothing
			}
			
			@Override
			public void lifeCycleStarted(LifeCycle event)
			{//nothing
			}
			
			@Override
			public void lifeCycleFailure(LifeCycle event, Throwable cause)
			{//nothing
			}
		});
	}

}
