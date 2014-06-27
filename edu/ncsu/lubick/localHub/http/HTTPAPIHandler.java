package edu.ncsu.lubick.localHub.http;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
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
			response.getWriter().println("Sorry, Nothing at this URL");
		}
		else if (target.length() < 5)			
		{
			response.getWriter().println("Sorry, Nothing at this URL");
		}
		else
		{
			handleGet(target, response);
		}
	}

	private void handleGet(String target, HttpServletResponse response) throws IOException
	{
		String[] pieces = target.split("/");
		/*
		 * /api/:creator/:app/:tool/:clip-name/'
		 * pieces[0] will be an empty string
		 * pieces[1] will be the the string "api"
		 */
		logger.info("Broken up target "+Arrays.toString(pieces));
		if (pieces.length <= 2) {
			response.getWriter().println("Sorry, Nothing at this URL");
		} else if (pieces.length == 3){
			handleEmailLeg(chopOffQueryString(pieces[2]), response);
		} else if (pieces.length == 5) {
			handleGetInfoAboutTool(pieces[3], chopOffQueryString(pieces[4]), response);
		}
	}

	private void handleGetInfoAboutTool(String applicationName, String toolName, HttpServletResponse response) throws IOException
	{
		List<File> keyClips = databaseLink.getBestExamplesOfTool(applicationName, toolName, true);
		List<File> guiClips = databaseLink.getBestExamplesOfTool(applicationName, toolName, false);
		ToolCountStruct countStruct = databaseLink.getToolAggregate(applicationName, toolName);
		
		JSONObject clips = new JSONObject();
		
		try
		{
			JSONArray keyJarr = new JSONArray();
			for(File f: keyClips)
			{
				keyJarr.put(f.getName());
			}
			
			JSONArray guiJarr = new JSONArray();
			for(File f: guiClips)
			{
				guiJarr.put(f.getName());
			}
			
			JSONObject usage = new JSONObject();
			JSONObject toolJson = new JSONObject();
			toolJson.put("gui", countStruct.guiToolCount);
			toolJson.put("keyboard", countStruct.keyboardCount);
			usage.put(toolName, toolJson);
			
			// Testing data
			keyJarr.put("Eclipse16274d13-bebb-3196-832c-70313e08cdaaK");
			//keyJarr.put("Eclipsee667cfd3-0bd8-3af8-93d7-10d16ab2f854");
			//guiJarr.put("Eclipsee434f382-7183-3cc5-8380-2137816a48d4");
			//guiJarr.put("Eclipse47397aaf-c70f-3aa1-9df5-a87f5a583af3");
			//guiJarr.put("Eclipse06ac5c3c-da64-3300-9a74-6fed83aa2722");

			
			clips.put("keyclips",keyJarr);
			clips.put("guiclips",guiJarr);
			clips.put("usage", usage);

			response.setContentType("application/json");
			clips.write(response.getWriter());
		}
		catch (JSONException e)
		{
			logger.error("Problem compiling clip names and writing them out "+clips,e);
		}
	}

	private void handleEmailLeg(String emailTarget, HttpServletResponse response) throws IOException
	{
		if (emailTarget.equals(userManager.getUserEmail())  || "user".equals(emailTarget) || "users".equals(emailTarget)) 
		{
			returnUserInfo(response);
		} 
		else 
		{
			response.getWriter().println("You can only query the local hub for information about you, "+ userManager.getUserName());
		}

	}

	private String chopOffQueryString(String target)
	{
		int indexOfQuery = target.indexOf('?');
		if (indexOfQuery != -1) {
			target = target.substring(0, indexOfQuery);
		}
		return target;
	}

	private void returnUserInfo(HttpServletResponse response) throws IOException
	{
		try {
			JSONObject user = makeUserAuthObj();

//			JSONObject applications = new JSONObject();
//			for(String plugin : this.databaseLink.getNamesOfAllPlugins()) {
//				applications.put(plugin, makePluginArray(plugin));
//			}
//			user.put("applications", applications);
			
			JSONObject data = new JSONObject();
			data.put("user", user);
			
			data.write(response.getWriter());
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
