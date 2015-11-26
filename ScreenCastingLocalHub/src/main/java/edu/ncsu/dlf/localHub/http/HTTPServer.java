package edu.ncsu.dlf.localHub.http;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import edu.ncsu.dlf.localHub.UserManager;
import edu.ncsu.dlf.localHub.WebQueryInterface;

public class HTTPServer {

	// HTTP Port Number
	public static final int SERVER_PORT = 4443;	

	// information about the HTTP server
	public static final String VERSION_ID = "screencasting-2.4.6-RC";

	private static Logger logger = Logger.getLogger(HTTPServer.class.getName());
	
	private static UserManager userManager = null;

	private Server underlyingServer;

	private HTTPServer()
	{
	}

	public static UserManager getUserManager()
	{
		return userManager;
	}
	
	public static HTTPServer startUpAnHTTPServer(WebQueryInterface wqi, UserManager um)
	{
		HTTPServer httpServer = new HTTPServer();
		if (HTTPServer.getUserManager() == null)
		{
			userManager = um;
		}
		httpServer.underlyingServer = new Server(new InetSocketAddress("localhost", SERVER_PORT));
		//httpServer.underlyingServer = new Server(SERVER_PORT);  this can be uncommented for dev
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

	public void shutDown()
	{
		try
		{
			underlyingServer.stop();
		}
		catch (Exception e)
		{
			logger.error("Problem shutting down HTTP Server", e);
		}
	}

}
