package edu.ncsu.lubick.localHub.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.UserManager;

public class HTTPUtils {

	private static final Logger logger = Logger.getLogger(HTTPUtils.class);

	public static final String BASE_URL = "recommender.oscar.ncsu.edu/api/v2";

	@Deprecated
	public static URI buildExternalHttpURI(String path, UserManager um) throws URISyntaxException
	{

		URI firstPart = new URI("http", BASE_URL, path, null);

		// handles proper escaping of emails with + in them
		URIBuilder u = new URIBuilder(firstPart);
		u.addParameter("name", um.getUserName());
		u.addParameter("email", um.getUserEmail());
		u.addParameter("token", um.getUserToken());

		logger.debug("Built URI: " + u);

		return u.build();
	}

	public static URI buildExternalHttpURI(String path) throws URISyntaxException
	{
		return new URI("http", BASE_URL, path, null);
	}

	public static String getResponseBody(HttpResponse response) throws IOException, UnsupportedEncodingException
	{
		StringBuilder sb = new StringBuilder();
		InputStream ips = response.getEntity().getContent();
		try (BufferedReader buf = new BufferedReader(new InputStreamReader(ips, "UTF-8"));)
		{
			String s;
			while (true)
			{
				s = buf.readLine();
				if (s == null || s.length() == 0)
					break;
				sb.append(s);
			}
		}
		return sb.toString();
	}

	public static String chopOffQueryString(String target)
	{
		int indexOfQuery = target.indexOf('?');
		if (indexOfQuery != -1)
		{
			target = target.substring(0, indexOfQuery);
		}
		return target;
	}

	public static void addAuth(HttpMessage httpMessage, UserManager um)
	{
		String authString = String.format("%s|%s:%s", um.getUserEmail(),
				um.getUserName(), um.getUserToken());
		try
		{
			httpMessage.addHeader("Authorization", "Basic "+Base64.encodeBase64String(authString.getBytes("UTF-8")));
		}
		catch (UnsupportedEncodingException e)
		{
			logger.fatal("Problem encoding authString "+authString, e);
		}
	}

}
