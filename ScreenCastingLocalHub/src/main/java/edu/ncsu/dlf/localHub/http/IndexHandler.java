package edu.ncsu.dlf.localHub.http;

import java.io.IOException;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * This handler will be able to talk to the database.
 * 
 * @author KevinLubick
 * 
 */
public class IndexHandler extends AbstractHandler {

	private static final Logger logger = Logger.getLogger(IndexHandler.class);



	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!"/".equals(target))
		{
			return;
		}
		baseRequest.setHandled(true);
		logger.debug(String.format("HTML Request %s, with target %s", baseRequest.toString(), target));
		String type = baseRequest.getMethod();
		if ("POST".equals(type))
		{
			logger.error("/ doesn't know how to respond to a post");
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}
		else
		{
			respondToGet(response);
		}

	}

	// serve up the webpage with the list of tools
	private void respondToGet(HttpServletResponse response) throws IOException
	{
		response.setContentType("text/html;charset=utf-8");
		
		
		try(Scanner indexScanner = new Scanner(IndexHandler.class.getResourceAsStream("/public_html/index.html"));)
		{
			while (indexScanner.hasNextLine()) {
				response.getWriter().println(indexScanner.nextLine());
			}
		}
		catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Problem with index.html");
			logger.info("Problem with index.html", e);
		}

		response.setStatus(HttpServletResponse.SC_OK);
	}



}
