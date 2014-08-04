package edu.ncsu.lubick.localHub.http;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.ncsu.lubick.localHub.ClipOptions;
import edu.ncsu.lubick.localHub.WebQueryInterface;

public class HTTPClipSharer extends AbstractHandler {

	
	private static final String PARAM_END_FRAME = "end_frame";
	private static final String PARAM_START_FRAME = "start_frame";
	private static final String PARAM_RECIPIENT = "recipient";
	private static final String PARAM_CLIP_ID = "clip_id";
	private static final String PARAM_CROP_RECT = "crop_rect";
	private static final Logger logger = Logger.getLogger(HTTPClipSharer.class);
	private WebQueryInterface databaseLink;

	

	public HTTPClipSharer(WebQueryInterface wqi)
	{
		this.databaseLink = wqi;
	}



	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!"/shareClip".equals(target))
		{
			return;
		}
		
		baseRequest.setHandled(true);
		//response.setContentType("application/json");
		
		
		
		if ("POST".equals(baseRequest.getMethod()))
		{
			handlePost(request);
		}
		else 
		{
			logger.error("I don't know how to handle a POST like this");
			response.getWriter().println("Sorry, Nothing at this URL");
		}
		
	}



	private void handlePost(HttpServletRequest request)
	{
		logger.debug(request.getParameterMap());
		
		String clipId = request.getParameter(PARAM_CLIP_ID);
		String recipient = request.getParameter(PARAM_RECIPIENT);
		
		String paramStartFrame = request.getParameter(PARAM_START_FRAME);
		int startFrame = Integer.parseInt(paramStartFrame == null? "0" : paramStartFrame);
		String paramEndFrame = request.getParameter(PARAM_END_FRAME);
		int endFrame = Integer.parseInt(paramEndFrame == null? "0" : paramEndFrame);
		
		String cropX = request.getParameter(PARAM_CROP_RECT+"[x1]");
		String cropY = request.getParameter(PARAM_CROP_RECT+"[y1]");
		String cropWidth = request.getParameter(PARAM_CROP_RECT+"[width]");
		String cropHeight = request.getParameter(PARAM_CROP_RECT+"[height]");
		
		if (clipId == null || recipient == null)
		{
			logger.info("clipId = "+clipId +", recipient = "+recipient+", so cancelling");
			return;
		}
		
		Rectangle cropRect = null;
		if (cropX != null && cropY != null && cropWidth != null && cropHeight != null) {
			cropRect = new Rectangle(Integer.parseInt(cropX), Integer.parseInt(cropY),
					Integer.parseInt(cropWidth), Integer.parseInt(cropHeight));
		}
		
		
		this.databaseLink.shareClipWithUser(clipId, new ClipOptions(recipient, startFrame, endFrame, cropRect));
	}



}
