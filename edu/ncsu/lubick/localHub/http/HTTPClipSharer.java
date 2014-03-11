package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

public class HTTPClipSharer extends TemplateHandlerWithDatabaseLink {

	
	private static final String TEMPLATE_NAME = "shareClip.html";
	private static final Logger logger = Logger.getLogger(HTTPClipSharer.class);

	public HTTPClipSharer(String matchPattern)
	{
		super(matchPattern, null);

	}
	
	

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!this.strictCheckIfWeHandleThisRequest(target))
		{
			return;
		}
		logger.debug(request.getParameterMap());
		
		Map<Object, Object> model = addThisUserInfoToModel(new HashMap<>());
		
		processTemplate(response, model, TEMPLATE_NAME);
	
		baseRequest.setHandled(true);
	}



	@Override
	protected Logger getLogger()
	{
		return logger;
	}

}
