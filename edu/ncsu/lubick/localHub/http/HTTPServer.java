package edu.ncsu.lubick.localHub.http;


import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HTTPServer {


	// HTTP Port Number
	public static final int SERVER_PORT = 4443;

	// information about the HTTP server
	public static final String SERVER_NAME = "Social Screencasting";
	public static final String SERVER_VERSION = "0.1";
	public static final String SERVER_ETC = "now in Glorious No Color, with Handlers!";

	private static Logger logger = Logger.getLogger(HTTPServer.class.getName());

	private Server underlyingServer;


	private HTTPServer() {}

	public static HTTPServer startUpAnHTTPServer(WebQueryInterface wqi) 
	{
		HTTPServer httpServer = new HTTPServer();
		httpServer.underlyingServer = new Server(SERVER_PORT);
		httpServer.underlyingServer.setHandler(HandlerManager.makeHandler(wqi));

		try
		{
			httpServer.underlyingServer.start();
		} 
		catch (Exception e)
		{
			logger.error("There was a problem starting the server", e);
		}
		
		return httpServer;

	}



}
