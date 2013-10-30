package edu.ncsu.lubick.localHub.http;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceCollection;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HandlerManager
{
	private static Logger logger = Logger.getLogger(HandlerManager.class.getName());

	private static String[] staticResources = new String[] {
			"frontend/public_html/"
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

		ResourceCollection resourceCollection = new ResourceCollection(staticResources);
		ResourceHandler resourseHandler = new ResourceHandler();
		resourseHandler.setBaseResource(resourceCollection);
		resourseHandler.setDirectoriesListed(false);
		h.addHandler(resourseHandler);

		logger.debug("logger set up with 4 handlers");
		return h;
	}
}
