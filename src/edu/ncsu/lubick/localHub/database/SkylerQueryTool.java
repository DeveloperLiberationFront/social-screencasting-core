package edu.ncsu.lubick.localHub.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.lubick.localHub.ToolUsage;
import edu.ncsu.lubick.localHub.forTesting.TestingUtils;
import edu.ncsu.lubick.localHub.http.HTTPUtils;


/**
 * SkylerQueryTool is a simple java service that pulls tool usages from skylr
 * 
 *  skylrDestQueryURL	What is the URL to use when querying skylr?
 *  
 *  userID				What user executed the tool usages?
 *  
 * Optional properties:
 * 
 * @author John
 * History:
 * 20140701	Slankas	Initial version feature complete.
 * 
 * Possible future enhancements:
 * - Should use the build pattern to build up a query object, then apply the wrapper and send to skylr
 */
public class SkylerQueryTool{
	private static final Logger logger = Logger.getLogger(SkylerQueryTool.class);
	
	/** default name for the properties file to be loaded from the classpath */
	public static final String DEFAULT_PROPERTIES_FILE = "/EventForwarder.properties";
	
	/** system property name to be used to override the default config file */
	public static final String SYSTEM_CONFIG_LOCATION_NAME = "efConfig";

	public static final String PROPERTY_DEST_SKYLR_QUERY_URL = "skylrDestQueryURL";
		
	public static final String[] REQUIRED_PROPERTIES = { PROPERTY_DEST_SKYLR_QUERY_URL };

	private Properties queryProperties;
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	
	/**
	 * Loads the properties file from the default location.  It also allows from an override location to be used.
	 * 
	 * Note: if an IOexception is thrown, the exception is caught, a warning messages output
	 *       to the logs, and the process will continue to run.  If the required properties
	 *       are not present, then this will be handled by the validation check.
	 */
	public void loadProperties() {

		queryProperties = new java.util.Properties();
		try (InputStream propStream = SkylerQueryTool.class.getResourceAsStream(DEFAULT_PROPERTIES_FILE);)
		{	
			if (propStream != null) {
				queryProperties.load(propStream);
				logger.info("Loaded SkylerQueryTool properties file from default location: "+DEFAULT_PROPERTIES_FILE);  
			}
			else {
				logger.debug("SkylerQueryTool: loadProperties, unable to locate default properties file on classpath");				
			}
			
			// now see if the configuration file should be overriden
			String configFile = System.getProperty(SYSTEM_CONFIG_LOCATION_NAME);
			if (configFile != null) {
				try (FileInputStream configFOS = new FileInputStream(configFile)){
					queryProperties.load(configFOS);
				}
				logger.info("Loaded custom SkylerQueryTool properties from system property location at " + configFile); 
			}		
		}
		catch (IOException e) {  
			logger.error("SkylerQueryTool: loadProperties - IOException: "+e);
		}
	}
	
	/**
	 * Validates that the required properties are in place.  If the properties are not there,
	 * then the application halts with an return code.
	 * 
	 * If load properties has not been called, then the application will halt.
	 */
	public void validateProperties() {
		java.util.ArrayList<String> missingProperties = new java.util.ArrayList<String>();
		
		if (queryProperties == null) {
			throw new DBAbstractionException("SkylerQueryTool: validateProperties - loadProperties not yet called, exiting application");
		}
		
		for (String propName: REQUIRED_PROPERTIES) {
			if (queryProperties.getProperty(propName) == null) { missingProperties.add(propName); 	}
		}
		
		if (missingProperties.size() > 0) {
			for (String missingProperty: missingProperties) {
				logger.fatal("EventForwarder: validatedProperties, missing property - "+ missingProperty);
			}
			throw new DBAbstractionException("EventForwarder: validateProperties, not all required properties present, exiting");
		}
	}
	
	
	/**
	 * Queries Skylr and returns a JSONObject representing the data.
	 * 
	 * A normal results includes these fields in the returned JSONObject:
	 * 		status - the HTTP status code from the server
	 * 		query  - the JSON query sent to Skylr
	 *      results - a JSON array of the found data
	 *      
	 * If the server returned an error code >= 400, only the status and query fields are set.
	 * If an exception occurs, the "exception" field is set for the result object.  The value is the exception message
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	private JSONObject querySkylr(JSONObject query) {
		JSONObject result = new JSONObject();
		
		HttpPost postRequest = new HttpPost(queryProperties.getProperty(PROPERTY_DEST_SKYLR_QUERY_URL));
		try {
			StringEntity input = new StringEntity(query.toString());
			input.setContentType("application/json");
			postRequest.setEntity(input);
			 
			HttpResponse response = httpClient.execute(postRequest);
			result.put("status", response.getStatusLine().getStatusCode());
			result.put("query", query);
			
			if (response.getStatusLine().getStatusCode() >= 400) {
				logger.warn("Skylr - unable to search - "+ response.getStatusLine().getStatusCode() +": query: "+query);
			}
			else {
				
				String responseBody = HTTPUtils.getResponseBody(response);
	
				JSONArray responseObject = new JSONArray("["+responseBody+"]");
				
				result.put("results", responseObject);
			}
		}
		catch (Exception e) {
			try {
				result.put("exception", e.getMessage());
			}
			catch (JSONException je) {
				logger.warn("Skylr - unable to store exception message in JSON Object");
			}
			logger.warn("Skylr - unable in find existing object  ("+ e.getMessage() +") - query: "+query);
		}
		finally {
			postRequest.releaseConnection();
		}

		return result; 
	}
	

	public JSONObject queryByToolUseID(ToolUsage toolUsage) throws JSONException {
		JSONObject query = new JSONObject();
		query.put("data.AppData.rcdOriginalID", toolUsage.getUseID());
		
		JSONObject wrapperObject = new JSONObject();
		wrapperObject.put("type", "find");
		wrapperObject.put("query", query);
		
		return querySkylr(wrapperObject);
	}	
	
	/**
	 * Queries Skylr for all records matching the project ID and returns a JSONObject representing the data.
	 * 
	 * A normal results includes these fields in the returned JSONObject:
	 * 		status - the HTTP status code from the server
	 * 		query  - the JSON query sent to Skylr
	 *      results - a JSON array of the found data
	 *      
	 * If the server returned an error code >= 400, only the status and query fields are set.
	 * If an exception occurs, the "exception" field is set for the result object.  The value is the exception message
	 * 
	 * @param project ID
	 * @return
	 * @throws Exception
	 */
	public JSONObject queryByProjectID(String projectID) throws JSONException {
		JSONObject query = new JSONObject();
		query.put("data.ProjId", projectID);
		
		JSONObject wrapperObject = new JSONObject();
		wrapperObject.put("type", "find");
		wrapperObject.put("query", query);
		
		return querySkylr(wrapperObject);		
		
		
	}

    public static void main(String args[]) {
    	TestingUtils.makeSureLoggingIsSetUp();
    	
    	SkylerQueryTool sqt = new SkylerQueryTool();
    	
    	try {
    		sqt.loadProperties();
    		sqt.validateProperties();
    		
    		JSONObject results = sqt.queryByProjectID("LAS/Recommender");
    		System.out.print(results);
    	}
    	catch (Exception e) {
    		System.err.println(e);
    		e.printStackTrace();
    	}
    }
    

}
