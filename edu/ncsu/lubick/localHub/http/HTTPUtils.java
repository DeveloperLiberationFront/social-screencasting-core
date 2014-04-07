package edu.ncsu.lubick.localHub.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.UserManager;

public class HTTPUtils {
	
	
	private static final Logger logger = Logger.getLogger(HTTPUtils.class);

	public static final String BASE_URL = "screencaster-hub.appspot.com";

	@Deprecated
	public static List<NameValuePair> assembleUserObject(UserManager userManager)
	{
		List<NameValuePair> retVal = new ArrayList<>();
		retVal.add(new BasicNameValuePair("name", userManager.getUserName()));
		retVal.add(new BasicNameValuePair("email", userManager.getUserEmail()));
		retVal.add(new BasicNameValuePair("token", userManager.getUserToken()));
		return retVal;
	}
	
	@Deprecated
	public static String getEscapedUserAuthURL(UserManager userManager)
	{
		List<NameValuePair> userObject = HTTPUtils.assembleUserObject(userManager);
		return URLEncodedUtils.format(userObject, "UTF-8");
	}

	@Deprecated
	public static String getUnEscapedUserAuthURL(UserManager userManager)
	{
		
		StringBuilder sb = new StringBuilder();
		sb.append("name=");
		sb.append(userManager.getUserName());
		sb.append("&email=");
		sb.append(userManager.getUserEmail());
		sb.append("&token=");
		sb.append(userManager.getUserToken());
		return sb.toString();
		
	}
	
	public static URI buildURI(String scheme, String host, String path, UserManager um) throws URISyntaxException {
		
		URI firstPart = new URI(scheme, host, path, null);
		
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
			while(true )
		    {
		        s = buf.readLine();
		        if(s==null || s.length()==0)
		            break;
		        sb.append(s);
	
		    }
		
			
		}
		return sb.toString();
	}

}
