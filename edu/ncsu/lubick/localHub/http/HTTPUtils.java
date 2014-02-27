package edu.ncsu.lubick.localHub.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import edu.ncsu.lubick.localHub.UserManager;

public class HTTPUtils {

	public static List<NameValuePair> assembleUserObject(UserManager userManager)
	{
		List<NameValuePair> retVal = new ArrayList<>();
		retVal.add(new BasicNameValuePair("name", userManager.getUserName()));
		retVal.add(new BasicNameValuePair("email", userManager.getUserEmail()));
		retVal.add(new BasicNameValuePair("token", userManager.getUserToken()));
		return retVal;
	}

	public static String getUserAuthURL(UserManager userManager)
	{
		List<NameValuePair> userObject = HTTPUtils.assembleUserObject(userManager);
		return URLEncodedUtils.format(userObject, "UTF-8");
	}

}
