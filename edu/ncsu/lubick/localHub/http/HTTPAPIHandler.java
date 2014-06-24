package edu.ncsu.lubick.localHub.http;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.UserManager;
import edu.ncsu.lubick.localHub.WebQueryInterface;
import edu.ncsu.lubick.util.ToolCountStruct;

public class HTTPAPIHandler extends AbstractHandler {

	private static final Logger logger = Logger.getLogger(HTTPAPIHandler.class);
	private WebQueryInterface databaseLink;
	private UserManager userManager = HTTPServer.getUserManager();

	public HTTPAPIHandler(WebQueryInterface wqi)
	{
		this.databaseLink = wqi;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (!target.startsWith("/api")) {
			return;
		}
		baseRequest.setHandled(true);
		response.setContentType("application/json");

		String type = baseRequest.getMethod();

		if ("POST".equals(type))
		{
			logger.error("I don't know how to handle a POST like this");
		}
		else if (target.length() < 5)			
		{
			response.getWriter().println("Sorry, Nothing at this URL");
		}
		else
		{
			handleEmailLeg(target.substring(5), response);	//5 letters in /api/
		}
	}

	private void handleEmailLeg(String emailTarget, HttpServletResponse response) throws IOException
	{
		if ("".equals(emailTarget)) {
			response.getWriter().println("Sorry, Nothing at this URL");
			return;
		}
		if (-1 == emailTarget.indexOf('/')) 
		{
			if (emailTarget.equals(userManager.getUserEmail())  || "user".equals(emailTarget) || "users".equals(emailTarget)) 
			{
				returnUserInfo(response);
			} 
			else 
			{
				response.getWriter().println("You can only query the local hub for information about you, "+ userManager.getUserName());
			}
		} else {
			response.getWriter().println("Sorry, Nothing at this URL");
		}
	}

	private void returnUserInfo(HttpServletResponse response) throws IOException
	{
		try {
			JSONObject user = makeUserAuthObj();

			JSONObject applications = new JSONObject();
			for(String plugin : this.databaseLink.getNamesOfAllPlugins()) {
				applications.put(plugin, makePluginArray(plugin));
			}
			user.put("applications", applications);
			
			user.write(response.getWriter());
		}
		catch (JSONException e) {
			throw new IOException("Problem making JSON", e);
		}
	}

	private JSONArray makePluginArray(String pluginName)
	{
		List<ToolCountStruct> counts = databaseLink.getAllToolAggregateForPlugin(pluginName);

		JSONArray retVal = new JSONArray();
		for(ToolCountStruct tcs: counts)
		{
			JSONObject tempObject = new JSONObject();
			try
			{
				tempObject.put("name", tcs.toolName);
				tempObject.put("gui", tcs.guiToolCount);
				tempObject.put("keyboard", tcs.keyboardCount);
				retVal.put(tempObject);
			}
			catch (JSONException e)
			{
				logger.error("Unusual JSON exception, squashing: ",e);
			}
		}
		return retVal;
	}

	private JSONObject makeUserAuthObj() throws JSONException
	{
		JSONObject user = new JSONObject();
		user.put("email", userManager.getUserEmail());
		user.put("name", userManager.getUserName());
		user.put("token", userManager.getUserToken());
		return user;
	}

}
