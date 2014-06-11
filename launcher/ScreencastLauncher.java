import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

public class ScreencastLauncher
{
	
	private static CloseableHttpClient client = HttpClients.createDefault();
	
	public static void main(String[] args) throws URISyntaxException
	{
		String currentLatest = "";
		try
		{
			currentLatest = findAndExecuteLatestVersion();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;	//abort execution
		}
		catch (NoExecutablesFoundException e)
		{
			e.printStackTrace();
			//fall through to try to update
		}
		
		searchForUpdate(currentLatest);
	}

	private static void searchForUpdate(String currentLatest) throws URISyntaxException
	{
		waitOneMinute();		//this normally gets run at boot.  We don't want to delay a user's start up process too much
		
		HttpGet get = new HttpGet(makeVersionURI());
		JSONObject responseObject = getVersionJSON(get);
		String serverVersion = responseObject.optString("version", "Screencasting-0.0.0-RELEASE.jar");
		if (serverVersion.compareToIgnoreCase(currentLatest) <= 0) {
			System.out.println("We are up to date "+serverVersion+" <= "+currentLatest);
			return;
		}
		
		downloadLatestVersion(get, serverVersion);
	}

	private static void downloadLatestVersion(HttpGet get, String serverVersion) throws URISyntaxException
	{
		get.setURI(makeDownloadURI(serverVersion));
		try(CloseableHttpResponse response = client.execute(get);)
		{
			writeResponseToFile(response, new File("./"+serverVersion));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			get.releaseConnection();
		}	
	}

	private static void writeResponseToFile(CloseableHttpResponse response, File file) throws IllegalStateException, IOException
	{
		InputStream in = response.getEntity().getContent();
		if (file.exists() || file.createNewFile())
		{
			try(FileOutputStream fos = new FileOutputStream(file);)
			{
				byte[] buffer = new byte[4096];
				int length; 
				while((length = in.read(buffer)) > 0) {
					fos.write(buffer, 0, length);
				}
			}
		}
		else {
			System.err.println("Couldn't make file "+file);
		}
	}

	private static JSONObject getVersionJSON(HttpGet get)
	{
		try(CloseableHttpResponse response = client.execute(get);)
		{
			String responseBody = getResponseBodyAsString(response);
			return new JSONObject(responseBody);
		}
		catch (JSONException | IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			get.releaseConnection();
		}
		return new JSONObject();
	}

	private static void waitOneMinute()
	{
		try
		{
			Thread.sleep(60_000);
		}
		catch (InterruptedException e1)
		{
			e1.printStackTrace();
		}
	}

	private static URI makeVersionURI() throws URISyntaxException
	{
		return new URI("http", "screencaster-hub.appspot.com", "api/update", null);
	}

	private static URI makeDownloadURI(String serverVersion) throws URISyntaxException
	{
		return new URI("http", "screencaster-hub.appspot.com", "api/file/"+serverVersion, null);
	}

	private static String findAndExecuteLatestVersion() throws IOException, NoExecutablesFoundException
	{
		File f = new File(".");
		String[] fileNames = f.list();
		Arrays.sort(fileNames);
		for(int i = fileNames.length-1;i>=0;i--) {
			String fileName = fileNames[i];
			if (fileName.startsWith("Screencasting-") && fileName.endsWith(".jar")) {
				ProcessBuilder pBuilder = new ProcessBuilder("java","-jar",fileName);
				pBuilder.directory(f);
				pBuilder.start();
				return fileName;
			}
		}
		throw new NoExecutablesFoundException("Could not find any jars to execute in "+f+" : "+Arrays.toString(fileNames));
		
	}
	
	public static String getResponseBodyAsString(HttpResponse response) throws IOException, UnsupportedEncodingException
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
	
	
	private static class NoExecutablesFoundException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6244266598099158331L;

		public NoExecutablesFoundException(String string)
		{
			super(string);
		}
		
	}
}


