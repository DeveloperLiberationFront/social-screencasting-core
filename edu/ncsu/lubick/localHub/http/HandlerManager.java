package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HandlerManager
{
	private static Logger logger = Logger.getLogger(HandlerManager.class.getName());

	private static String[] staticResourcePaths = new String[] {
			"src/frontend/public_html/",
			"renderedVideos/"
	};

	private HandlerManager()
	{
	}

	public static Handler makeHandler(WebQueryInterface wqi)
	{
		HandlerCollection h = new HandlerList();

		h.addHandler(new LookupHandler("/", wqi));
		h.addHandler(new LookupHandler("/index", wqi));
		h.addHandler(new VideoCreator("/makeVideo", wqi));
		if (wqi instanceof WebToolReportingInterface)
		{
			h.addHandler(new ToolReportingHandler("/reportTool", (WebToolReportingInterface)wqi ));
		}
		h.addHandler(new VersionHandler("/version"));
		
		Resource[] staticResources = new Resource[staticResourcePaths.length];
		int i = 0;
		for(String resourcePath : staticResourcePaths)
		{
			URL url = h.getClass().getResource(resourcePath);
			try
			{
				staticResources[i] = Resource.newResource(url);
			}
			catch (IOException e)
			{
				logger.error("Resource not found: "+resourcePath, e);
			}
			i++;
		}
		
		ResourceCollection resourceCollection = new ResourceCollection(staticResources);
		ResourceHandler resourseHandler = new ResourceHandler();
		resourseHandler.setBaseResource(resourceCollection);
		resourseHandler.setDirectoriesListed(false);
		h.addHandler(resourseHandler);

		logger.debug("logger set up with 4 handlers");
		return h;
	}
}
