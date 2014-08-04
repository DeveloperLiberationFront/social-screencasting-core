package edu.ncsu.lubick.localHub.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.UserManager;

public class HTTPUtils {
	
	
	private static final Logger logger = Logger.getLogger(HTTPUtils.class);

	public static final String BASE_URL = "screencaster-hub.appspot.com";
	
	public static URI buildExternalHttpURI(String path, UserManager um) throws URISyntaxException {
		
		URI firstPart = new URI("http", BASE_URL, path, null);
		
		//handles proper escaping of emails with + in them
		URIBuilder u = new URIBuilder(firstPart);
		u.addParameter("name", um.getUserName());
		u.addParameter("email", um.getUserEmail());
		u.addParameter("token", um.getUserToken());
		
		logger.debug("Built URI: "+u);
		
		return u.build();
	}
	

	public static String getResponseBody(HttpResponse response) throws IOException, UnsupportedEncodingException
	{
		StringBuilder sb = new StringBuilder();
		InputStream ips  = response.getEntity().getContent();
		try(BufferedReader buf = new BufferedReader(new InputStreamReader(ips,"UTF-8"));)
		{	
		    String s;
			while (true) {
		        s = buf.readLine();
		        if(s==null || s.length()==0)
		            break;
		        sb.append(s);
		    }
		}
		return sb.toString();
	}
	
	public static String chopOffQueryString(String target)
	{
		int indexOfQuery = target.indexOf('?');
		if (indexOfQuery != -1) {
			target = target.substring(0, indexOfQuery);
		}
		return target;
	}
	
	public static String getRequestBody(HttpServletRequest request) throws IOException{
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
		String data;
		while((data= br.readLine())!=null){
			sb.append(data).append('\n');
		}	
		return sb.toString();
	}
	
	public static JSONObject getRequestJSON(HttpServletRequest request) throws JSONException, IOException{
		String jsonString=getRequestBody(request);
		return new JSONObject(jsonString);
	}
}
