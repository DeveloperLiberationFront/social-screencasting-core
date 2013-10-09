package edu.ncsu.lubick.localHub.http;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HandlerManager 
{
	private static Logger logger = Logger.getLogger(HandlerManager.class.getName());


	private HandlerManager() {}

	public static Handler makeHandler(WebQueryInterface wqi) {
		HandlerCollection h = new HandlerList();
		
		h.addHandler(new LookupHandler("/", wqi));
		h.addHandler(new LookupHandler("/index", wqi));
		
		//Put this last, else it will try and fail to list the directory on "/" match
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(false);
		resourceHandler.setResourceBase("frontend/public_html/");
		h.addHandler(resourceHandler);
		
		logger.debug("logger set up with 3 handlers");
		return h;
	}
}