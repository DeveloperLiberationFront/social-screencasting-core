package edu.ncsu.lubick.localHub.videoPostProduction;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Uploads a browser media package to app engine
 * @author KevinLubick
 *
 */
public class BrowserMediaPackageUploader {

	private CloseableHttpClient client = HttpClients.createDefault();
	
	//For whitebox/end-to-end testing
	public static void main(String[] args)
	{
		
	}
	
	
	
}
