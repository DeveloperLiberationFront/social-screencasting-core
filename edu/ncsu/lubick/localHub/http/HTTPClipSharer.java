package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HTTPClipSharer extends TemplateHandlerWithDatabaseLink {

	
	private static final String PARAM_RECIPIENT = "recipient";
	private static final String PARAM_CLIP_ID = "clipId";
	private static final String TEMPLATE_NAME = "shareClip.html";
	private static final Logger logger = Logger.getLogger(HTTPClipSharer.class);

	public HTTPClipSharer(String matchPattern, WebQueryInterface wqi) throws IOException, URISyntaxException
	{
		super(matchPattern, wqi);

	}
	
	

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!this.strictCheckIfWeHandleThisRequest(target))
		{
			return;
		}
		logger.debug(request.getParameterMap());
		
		if ("POST".equals(baseRequest.getMethod()))
		{
			handlePost(baseRequest, request);
		}
		else 
		{
			handleGet(baseRequest, response);
		}
		
	}



	private void handlePost(Request baseRequest, HttpServletRequest request)
	{
		baseRequest.setHandled(true);
		String clipId = request.getParameter(PARAM_CLIP_ID);
		String recipient = request.getParameter(PARAM_RECIPIENT);
		int startFrame = Integer.parseInt(request.getParameter("start_frame"));
		int endFrame = Integer.parseInt(request.getParameter("end_frame"));
		if (clipId == null || recipient == null)
		{
			logger.info("clipId = "+clipId +", recipient = "+recipient+", so cancelling");
			
			return;
		}
		this.databaseLink.shareClipWithUser(clipId, recipient, startFrame, endFrame);
	}



	private void handleGet(Request baseRequest, HttpServletResponse response) throws IOException
	{
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
