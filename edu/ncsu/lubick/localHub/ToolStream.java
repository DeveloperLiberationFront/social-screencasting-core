package edu.ncsu.lubick.localHub;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ToolStream {

	public static final String TOOL_NAME = "Tool_Name";
	public static final String TOOL_CLASS = "Tool_Class";
	public static final String TOOL_KEY_PRESSES = "Tool_Key_Presses";
	public static final String TOOL_TIMESTAMP = "Tool_Timestamp";
	public static final String TOOL_DURATION = "Tool_Duration";
	
	
	private List<ToolUsage> listOfToolUsages;
	private Date timeStamp;
	private String pluginName;
	
	private static Logger logger = Logger.getLogger(ToolStream.class.getName());

	protected ToolStream() {
		listOfToolUsages = new ArrayList<>();
	}

	public static ToolStream generateFromJSON(String fileContents) {
		JSONArray jArray;
		try {
			jArray = new JSONArray(fileContents);
		} catch (JSONException e) {
			logger.error("Problem reading in from JSON", e);
			return null;
		}
		
		ToolStream ts = new ToolStream();
		
		for(int i = 0; i< jArray.length(); i++)
		{
			JSONObject jobj;
			ToolUsage tu;
			try {
				jobj = jArray.getJSONObject(i);
				tu = ToolUsage.buildFromJSONObject(jobj);
				
			} catch (JSONException e) {
				logger.error("Malformed JSON.  Skipping", e);
				continue;
			}
			
			ts.listOfToolUsages.add(tu);
		}
		
		return ts;
	}
	
	public List<ToolUsage> getAsList() {
		return this.listOfToolUsages;
	}
	
	public static class ToolUsage {
		
		private String toolName, toolClass, keyPresses;
		private Date timeStamp;
		private int duration;
		private String pluginName;
		
		private ToolUsage(String toolName, String toolClass, String keyPresses, Date timeStamp, int duration) 
		{
			this.toolName = toolName.trim();
			this.toolClass = toolClass.trim();
			this.keyPresses = keyPresses.trim();
			this.timeStamp = timeStamp;
			this.duration = duration;
		}
		
		public ToolUsage(String toolName, String toolClass, String keyPresses, String pluginName, Date timeStamp, int duration) 
		{
			this(toolName, toolClass, keyPresses, timeStamp, duration);
			setPluginName(pluginName);
		}

		public static ToolUsage buildFromJSONObject(JSONObject jobj) throws JSONException
		{
			return new ToolUsage(jobj.getString(ToolStream.TOOL_NAME), jobj.getString(ToolStream.TOOL_CLASS),
					jobj.getString(ToolStream.TOOL_KEY_PRESSES), new Date(jobj.getLong(ToolStream.TOOL_TIMESTAMP)),
					jobj.getInt(ToolStream.TOOL_DURATION));
		}

		public String getToolName() {
			return toolName;
		}

		public String getToolClass() {
			return toolClass;
		}

		public String getToolKeyPresses() {
			return keyPresses;
		}

		public Date getTimeStamp() {
			return timeStamp;
		}

		public int getDuration() {
			return duration;
		}

		public String getPluginName() {
			return this.pluginName;
		}
		
		public void setPluginName(String pluginName)
		{
			this.pluginName = pluginName;
		}

	}

	public void setTimeStamp(Date associatedDate) {
		this.timeStamp = associatedDate;
	}
	public Date getTimeStamp()
	{
		return this.timeStamp;
	}

	public void setAssociatedPlugin(String pluginName) {
		this.pluginName = pluginName;
		for(ToolUsage tu: this.listOfToolUsages)
		{
			tu.setPluginName(pluginName);
		}
	}

	public String getAssociatedPlugin() {
		return pluginName;
	}
}
