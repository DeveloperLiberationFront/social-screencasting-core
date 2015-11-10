package edu.ncsu.lubick.localHub.http;

import java.awt.Rectangle;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.json.JSONObject;

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
		
		baseRequest.setHandled(true);
		//response.setContentType("application/json");
		
		
		
		if ("POST".equals(baseRequest.getMethod()))
		{
			try
			{
				handlePost(request);
			}
			catch (JSONException e)
			{
				response.getWriter().println("There was a problem");
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
		else 
		{
			logger.error("I don't know how to handle a POST like this");
			response.getWriter().println("Sorry, Nothing at this URL");
		}
		
	}



	private void handlePost(HttpServletRequest request) throws JSONException
	{
		JSONObject jobj = HTTPUtils.getRequestJSON(request).getJSONObject("data");
		logger.debug(jobj);
		
		if (jobj == null) {
			logger.info("no data, so cancelling");
			return;
		}
		
		String clipId = jobj.optString(PARAM_CLIP_ID);
		String recipient = jobj.optString(PARAM_RECIPIENT);
		
		int startFrame = jobj.optInt(PARAM_START_FRAME, 0);
		int endFrame = jobj.optInt(PARAM_END_FRAME, 0);
		
		if (clipId.isEmpty() || recipient.isEmpty())
		{
			logger.info("clipId = "+clipId +", recipient = "+recipient+", so cancelling");
			return;
		}
		
		JSONObject cropBox = null;
		try
		{
			cropBox = new JSONObject(jobj.optString(PARAM_CROP_RECT, "{}"));
		}
		catch (JSONException e)
		{
			logger.warn("Problem with cropbox", e);
		}
		
		Rectangle cropRect = null;
		if (cropBox != null && cropBox.has("x1") && cropBox.has("y1") && cropBox.has("width") && cropBox.has("height") ) {
			cropRect = new Rectangle(cropBox.getInt("x1"), cropBox.getInt("y1"),
					cropBox.getInt("width"), cropBox.getInt("height"));
		}
		
		
		this.databaseLink.shareClipWithUser(clipId, new ClipOptions(recipient, startFrame, endFrame, cropRect));
	}



}
