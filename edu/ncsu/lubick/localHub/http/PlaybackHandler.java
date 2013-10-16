package edu.ncsu.lubick.localHub.http;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.ncsu.lubick.localHub.WebQueryInterface;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public class PlaybackHandler extends AbstractHandler {

	private static Logger logger;
	private static Configuration cfg;		//Template Configuration
	
	
	private String httpRequestPattern;
	private WebQueryInterface databaseLink;
	
	static {
		logger = Logger.getLogger(LookupHandler.class.getName());
		try {
			logger.trace("Setting up template configuration");
			setupTemplateConfiguration();
		} catch (IOException e) {
			logger.fatal("There was a problem booting up the template configuration");
		}
	}


	
	public PlaybackHandler(String matchPattern, WebQueryInterface databaseLink) 
	{
		this.httpRequestPattern = matchPattern;
		this.databaseLink = databaseLink;
	}

	private static void setupTemplateConfiguration() throws IOException {
		cfg = new Configuration();
		cfg.setDirectoryForTemplateLoading(new File("./frontend/templates"));
		cfg.setObjectWrapper(new DefaultObjectWrapper());
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));  // FreeMarker 2.3.20 
		
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException 
	{
		if (!target.equals(httpRequestPattern))
		{
			logger.debug("LookupHandler passing on target "+target);
			return;
		}

		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);


		processTemplate(response, null, "playback.html");
		
	}

	private void processTemplate(HttpServletResponse response, Map<Object, Object> templateData, String templateName) throws IOException 
	{
		Template temp = cfg.getTemplate(templateName);

		try {
			temp.process(templateData, response.getWriter());
		} catch (TemplateException e) {
			logger.error("Problem with the template",e);
		}
	}


}
