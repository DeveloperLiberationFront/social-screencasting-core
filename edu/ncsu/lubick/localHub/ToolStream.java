package edu.ncsu.lubick.localHub;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
	public static final String MENU_KEY_PRESS = "[GUI]";
	public static final String TOOL_START = "Tool_Start";
	public static final String TOOL_END = "Tool_End";
	public static final String TOOL_SCORE = "Tool_Score";
	

	private List<ToolUsage> listOfToolUsages;
	private Date timeStamp;
	private String pluginName;

	private static Logger logger = Logger.getLogger(ToolStream.class.getName());

	protected ToolStream()
	{
		listOfToolUsages = new ArrayList<>();
	}

	public static String makeUniqueIdentifierForToolUsage(ToolUsage toolUsage, String userEmail)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(userEmail);
		String name = toolUsage.getPluginName();
		sb.append(name);
		sb.append(toolUsage.getToolName());
		sb.append(toolUsage.getTimeStamp().getTime());

		UUID u;
		try
		{
			u = UUID.nameUUIDFromBytes(sb.toString().getBytes("UTF-8"));
			
			String lastLetter = MENU_KEY_PRESS.equals(toolUsage.keyPresses) ? "G" : "K";

			// add a truncatedName for readability (so we know, when browsing in the files,
			//what plugin it belongs to)
			String truncatedName = name.substring(0, name.length() >= 8 ? 8 : name.length());
			return truncatedName + u + lastLetter;
		}
		catch (UnsupportedEncodingException e)
		{
			logger.fatal("Severly wrong encoding", e);
		}
		return null;

	}

	public static ToolStream generateFromJSON(String fileContents)
	{
		JSONArray jArray;
		try
		{
			jArray = new JSONArray(fileContents);
			logger.debug("Array created: " + jArray.toString(1));
		}
		catch (JSONException e)
		{
			logger.error("Problem reading in from JSON", e);
			return null;
		}

		ToolStream ts = new ToolStream();

		for (int i = 0; i < jArray.length(); i++)
		{
			JSONObject jobj;
			ToolUsage tu;
			try
			{
				jobj = jArray.getJSONObject(i);
				tu = ToolUsage.buildFromJSONObject(jobj);

			}
			catch (JSONException e)
			{
				logger.error("Malformed JSON.  Skipping", e);
				continue;
			}

			ts.listOfToolUsages.add(tu);
		}

		return ts;
	}

	public List<ToolUsage> getAsList()
	{
		return this.listOfToolUsages;
	}

	public void setTimeStamp(Date associatedDate)
	{
		this.timeStamp = associatedDate;
	}

	public Date getTimeStamp()
	{
		return this.timeStamp;
	}

	public void setAssociatedPlugin(String pluginName)
	{
		this.pluginName = pluginName;
		for (ToolUsage tu : this.listOfToolUsages)
		{
			tu.setPluginName(pluginName);
		}
	}

	public String getAssociatedPlugin()
	{
		return pluginName;
	}

	@Override
	public String toString()
	{
		return "ToolStream [listOfToolUsages=" + listOfToolUsages + ", timeStamp=" + timeStamp + ", pluginName=" + pluginName + "]";
	}

	public static class ToolUsage {
		
		private String useID;
		
		private String toolName, toolClass, keyPresses;
		private Date timeStamp;
		private int duration;
		private String pluginName;
		private int clipScore;
		private int startFrame;
		private int endFrame;
		private String startData;
		private String endData;
		private String ratingData;

		private ToolUsage(String toolName, String toolClass, String keyPresses, Date timeStamp, int duration, int clipScore)
		{
			if(toolName != null)
				this.toolName = toolName.trim();
			if(toolClass != null)
				this.toolClass = toolClass.trim();
			if(keyPresses != null)
				this.keyPresses = keyPresses.trim();
			
			this.timeStamp = timeStamp;
			this.duration = duration;
			this.clipScore = clipScore;
		}

		public ToolUsage(String toolName, String toolClass, String keyPresses, String pluginName, Date timeStamp, int duration, int score)
		{
			this(toolName, toolClass, keyPresses, timeStamp, duration, score);
			setPluginName(pluginName);
		}

		public ToolUsage(String useID, String toolName, String toolClass, String keyPresses, String pluginName, Date timeStamp, int duration, int score)
		{
			this(toolName, toolClass, keyPresses, timeStamp, duration, score);
			this.setPluginName(pluginName);
			this.setUseID(useID);
		}
		
		
		public static ToolUsage buildFromJSONObject(JSONObject jobj)
		{
			// TODO change this (eventually) to read the clip_score from the json

			String newToolName = jobj.optString(ToolStream.TOOL_NAME, "[No Name]");
			String newToolClass = jobj.optString(ToolStream.TOOL_CLASS, "");
			String newToolKeyPress = jobj.optString(ToolStream.TOOL_KEY_PRESSES, MENU_KEY_PRESS);
			Date newToolTimeStamp = new Date(jobj.optLong(ToolStream.TOOL_TIMESTAMP, 0));
			int newToolDuration = jobj.optInt(ToolStream.TOOL_DURATION, 0);
			double rating = jobj.optDouble(ToolStream.TOOL_SCORE, 0);

			ToolUsage tu = new ToolUsage(newToolName, newToolClass, newToolKeyPress, newToolTimeStamp, newToolDuration, (int) rating);

			try {
				JSONObject toolStartData = jobj.optJSONObject("Tool_Start_Data");
				String newToolStartData = toolStartData == null ? "{}": toolStartData.toString(2);
				
				JSONObject toolEndData = jobj.optJSONObject("Tool_End_Data");
				String newToolEndData = toolEndData == null ? "{}": toolEndData.toString(2);
				
				JSONObject ratingDataJobj = jobj.optJSONObject("rating_data");
				String ratingData = ratingDataJobj == null ? "{}": ratingDataJobj.toString(2);
				tu.setStartData(newToolStartData);
				tu.setEndData(newToolEndData);
				tu.setRatingData(ratingData);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return tu;
		}

		public String getRatingData() {
			return ratingData;
		}

		private void setRatingData(String ratingData) {
			this.ratingData = ratingData;
		}

		public String getStartData()
		{
			return startData;
		}
		
		public void setStartData(String newToolStartData) {
			startData = newToolStartData;
		}
		
		public String getEndData()
		{
			return endData;
		}

		public void setEndData(String newToolEndData) {
			endData = newToolEndData;
		}

		public String getToolName()
		{
			return toolName;
		}

		public String getToolClass()
		{
			return toolClass;
		}

		public String getToolKeyPresses()
		{
			return keyPresses;
		}

		public Date getTimeStamp()
		{
			return timeStamp;
		}

		public int getDuration()
		{
			return duration;
		}
		
		public int getStartFrame()
		{
			return startFrame;
		}
		
		public void setStartFrame(int startFrame) {
			this.startFrame = startFrame;
		}
		
		public int getEndFrame()
		{
			return endFrame;
		}
		
		public void setEndFrame(int endFrame) {
			this.endFrame = endFrame;
		}

		public String getPluginName()
		{
			return this.pluginName;
		}

		public final void setPluginName(String pluginName)
		{
			this.pluginName = pluginName;
		}

		public int getClipScore()
		{
			return this.clipScore;
		}
		
		public String getUseID() {
			return useID;
		}

		public void setUseID(String newUseID) {
			this.useID = newUseID;
		}		
		

		@Override
		public String toString()
		{
			return "ToolUsage [" + pluginName + "/" + toolName + ", duration=" + duration + "ms , clipScore=" + clipScore + ", timeStamp="
					+ timeStamp + "]";
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + clipScore;
			result = prime * result + duration;
			result = prime * result + ((keyPresses == null) ? 0 : keyPresses.hashCode());
			result = prime * result + ((pluginName == null) ? 0 : pluginName.hashCode());
			result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
			result = prime * result + ((toolClass == null) ? 0 : toolClass.hashCode());
			return prime * result + ((toolName == null) ? 0 : toolName.hashCode());

		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ToolUsage))
				return false;
			return this.hashCode() == obj.hashCode();

		}

	}
}
