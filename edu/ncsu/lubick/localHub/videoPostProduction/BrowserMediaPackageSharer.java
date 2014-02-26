package edu.ncsu.lubick.localHub.videoPostProduction;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.forTesting.TestingUtils;

public class BrowserMediaPackageSharer {
	
	
	private static final Logger logger = Logger.getLogger(BrowserMediaPackageSharer.class);
	private static CloseableHttpClient client = HttpClients.createDefault();

	public static void main(String[] args)
	{
		TestingUtils.makeSureLoggingIsSetUp();
		
		
	}
}
