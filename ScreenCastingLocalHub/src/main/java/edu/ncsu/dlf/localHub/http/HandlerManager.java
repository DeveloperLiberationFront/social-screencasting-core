package edu.ncsu.dlf.localHub.http;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;

import edu.ncsu.dlf.localHub.WebQueryInterface;
import edu.ncsu.dlf.localHub.videoPostProduction.PostProductionHandler;

public class HandlerManager
{
	private static Logger logger = Logger.getLogger(HandlerManager.class.getName());

	private static String[] staticWebResourcePaths = new String[] {
		"/public_html/",
		"/templates/"			// the folder src/frontend is being treated a source folder, so it is not exported
								// as /frontend/public_html and /frontend/templates, but as their own high-level folders
	};

	private static String renderedVideoPath = PostProductionHandler.MEDIA_OUTPUT_FOLDER;

	private HandlerManager()
	{
	}

	public static Handler makeHandler(WebQueryInterface wqi)
	{
		HandlerCollection h = new HandlerList();

		h.addHandler(new IndexHandler());
		h.addHandler(new HTTPAPIHandler(wqi));
		h.addHandler(new VersionHandler("/version"));

		h.addHandler(new HTTPToolReportingHandler((WebToolReportingInterface)wqi ));


		Resource[] staticWebResources = setUpWebResources(h.getClass());

		Resource[] allWebResources = setUpLocalMediaAssets(staticWebResources);

		logger.info("Web Resources "+Arrays.toString(allWebResources));

		makeAndHandlerForFiles(h, allWebResources);

		logger.debug("logger for HTTP Handlers set up with "+h.getChildHandlers().length+" handlers");
		return h;
	}

	private static Resource[] setUpWebResources(Class<?> classForJarResources)	//this will work whether running from Eclipse
	{																			//or in a jar
		Resource[] staticResources = new Resource[staticWebResourcePaths.length];	
		int i = 0;
		for(String resourcePath : staticWebResourcePaths)
		{
			URL url = classForJarResources.getResource(resourcePath);
			try
			{
				staticResources[i] = Resource.newResource(url);
			}
			catch (IOException e)
			{
				logger.error("Resource not found: "+resourcePath, e);
			}
			i++;
		}
		return staticResources;
	}

	private static Resource[] setUpLocalMediaAssets(Resource[] staticWebResources)
	{
		Resource[] retVal = Arrays.copyOf(staticWebResources, staticWebResources.length+1);
		
		try
		{
			File renderedVideoDir = new File(renderedVideoPath);
			if (renderedVideoDir.exists() || renderedVideoDir.mkdirs())
			{
				retVal[staticWebResources.length] = Resource.newResource(renderedVideoDir);
			}
			else
			{
				logger.error("Could not make the directory for the rendered video assets.  Won't serve them.");
			}
		}
		catch (IOException e)
		{
			logger.error("problem with the rendered video assets.  Leaving them out for now");
			return staticWebResources;
		}
		
		return retVal;
	}

	private static void makeAndHandlerForFiles(HandlerCollection h, Resource[] allWebResources)
	{
		ResourceCollection resourceCollection = new ResourceCollection(allWebResources);
		ResourceHandler resourseHandler = new ResourceHandler();
		resourseHandler.setBaseResource(resourceCollection);
		resourseHandler.setDirectoriesListed(false);
		h.addHandler(resourseHandler);
	}
}
