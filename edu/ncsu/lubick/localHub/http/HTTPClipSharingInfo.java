package edu.ncsu.lubick.localHub.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HTTPClipSharingInfo extends TemplateHandlerWithDatabaseLink {

	
	private static final Logger logger = Logger.getLogger(HTTPClipSharingInfo.class);
	
	public HTTPClipSharingInfo(String matchPattern, WebQueryInterface databaseLink)
	{
		super(matchPattern, databaseLink);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
