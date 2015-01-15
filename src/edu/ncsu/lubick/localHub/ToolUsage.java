package edu.ncsu.lubick.localHub;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class ToolUsage {
	public static final String MENU_KEY_PRESS = "[GUI]";
	
	
	public static final String TOOL_NAME = "Tool_Name";
	public static final String TOOL_CLASS = "Tool_Class";
	public static final String TOOL_KEY_PRESSES = "Tool_Key_Presses";
	public static final String TOOL_TIMESTAMP = "Tool_Timestamp";
	public static final String TOOL_DURATION = "Tool_Duration";
	public static final String TOOL_SCORE = "Tool_Score";
	
	private String useID;
	
	private String toolName, toolClass;

	private String keyPresses;
	private Date timeStamp;
	private int duration;
	private String applicationName;
	private int usageScore;
	
	private static Logger logger = Logger.getLogger(ToolUsage.class.getName());

	private ToolUsage(String toolName, String toolClass, String keyPresses, Date timeStamp, int duration, int usageScore)
	{
		if(toolName != null)
			this.toolName = toolName.trim();
		if(toolClass != null)
			this.toolClass = toolClass.trim();
		if(keyPresses != null)
			this.keyPresses = keyPresses.trim();
		
		this.timeStamp = timeStamp;
		this.duration = duration;
		this.usageScore = usageScore;
	}

	public ToolUsage(String toolName, String toolClass, String keyPresses, String pluginName, Date timeStamp, int duration, int score)
	{
		this(toolName, toolClass, keyPresses, timeStamp, duration, score);
		setApplicationName(pluginName);
	}

	public ToolUsage(String useID, String toolName, String toolClass, String keyPresses, String pluginName, Date timeStamp, int duration, int score)
	{
		this(toolName, toolClass, keyPresses, timeStamp, duration, score);
		this.setApplicationName(pluginName);
		this.setUseID(useID);
	}
	
	public static ToolUsage buildFromJSONObject(JSONObject jobj)
	{
		String newToolName = jobj.optString(TOOL_NAME, "[No Name]");
		String newToolClass = jobj.optString(TOOL_CLASS, "");
		String newToolKeyPress = jobj.optString(TOOL_KEY_PRESSES, MENU_KEY_PRESS);
		
		long timeInMillis = 0;
		String strToolTimeStamp = jobj.optString(TOOL_TIMESTAMP,"");
		if (strToolTimeStamp.isEmpty()) {
			timeInMillis = System.currentTimeMillis(); // if no time, default to the present.
		}
		else if (strToolTimeStamp.endsWith("-UTC")) {
			strToolTimeStamp = strToolTimeStamp.substring(0, strToolTimeStamp.indexOf("-UTC"));
			
			TimeZone tz = TimeZone.getDefault();
			timeInMillis = Long.parseLong(strToolTimeStamp) - tz.getRawOffset();
			logger.debug("corrected datetime: "+ new Date(timeInMillis));
		}
		else {
			timeInMillis = Long.parseLong(strToolTimeStamp);
		}
		
		Date newToolTimeStamp = new Date(timeInMillis);
		int newToolDuration = jobj.optInt(TOOL_DURATION, 0);
		int usageRating = jobj.optInt(TOOL_SCORE, newToolDuration);

		return new ToolUsage(newToolName, newToolClass, newToolKeyPress, 
				newToolTimeStamp, newToolDuration, usageRating);
	}

	public String makeUniqueIdentifierForToolUsage(String userEmail)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(userEmail);
		String name = this.getApplicationName();
		sb.append(name);
		sb.append(this.getToolName());
		sb.append(this.getTimeStamp().getTime());

		UUID u = UUID.nameUUIDFromBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
		
		String lastLetter = MENU_KEY_PRESS.equals(this.getToolKeyPresses()) ? "G" : "K";

		// add a truncatedName for readability (so we know, when browsing in the files,
		//what plugin it belongs to)
		String truncatedName = name.substring(0, name.length() >= 8 ? 8 : name.length());
		return truncatedName + u + lastLetter;
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

	public String getApplicationName()
	{
		return this.applicationName;
	}

	public final void setApplicationName(String pluginName)
	{
		this.applicationName = pluginName;
	}

	public int getUsageScore()
	{
		return this.usageScore;
	}
	
	public String getUseID() {
		return useID;
	}

	public final void setUseID(String newUseID) {
		this.useID = newUseID;
	}		
	

	@Override
	public String toString()
	{
		return "ToolUsage [useID=" + useID + ", toolName=" + toolName + ", toolClass=" + toolClass + ", keyPresses=" + keyPresses + ", timeStamp=" + timeStamp
				+ ", duration=" + duration + ", pluginName=" + applicationName + ", usageScore=" + usageScore + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + usageScore;
		result = prime * result + duration;
		result = prime * result + ((keyPresses == null) ? 0 : keyPresses.hashCode());
		result = prime * result + ((applicationName == null) ? 0 : applicationName.hashCode());
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