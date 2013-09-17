package org.lubick.localHub.forTesting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lubick.localHub.ToolStream;

/**
 * This is an idealized ToolStream producer, like a plugin.  Plugins should model the information
 * and the JSON output of this class. 
 * 
 * This is used for unit testing
 * 
 * For internal testing, there is an isEquivalent() function that can compare an IdealizedToolStream
 * to the internal representation of the tool stream
 * @author Kevin Lubick
 *
 */
public class IdealizedToolStream 
{
	
	private static Logger logger = Logger.getLogger(IdealizedToolStream.class.getName());
	
	private List<ToolUsage> listOfToolUsages;

	public IdealizedToolStream() {
		listOfToolUsages = new ArrayList<>();
	}

	public static IdealizedToolStream generateRandomToolStream(int numberOfCommands) {
		// TODO Auto-generated method stub
		return new IdealizedToolStream();
	}

	/**
	 * This should spit out a jsonArray of json objects.
	 * 
	 * Each JsonObject should have a String paired with "Tool_Name", a
	String paired with "Tool_Class", a String paired with "Tool_Key_Presses",
	a long paired with "Tool_Timestamp" (number of milliseconds since epoch),
	and an int paired with "Tool_Duration";
	 * @return
	 */
	public String toJSON() {
		JSONArray jarr = new JSONArray();
		for (ToolUsage tu : this.listOfToolUsages) 
		{
			try {
				jarr.put(tu.toJSONObject());
			} catch (JSONException e) {
				logger.error("There was a problem converting to JSON",e);
				return null;
			}
		}
		String retVal = null;
		try {
			retVal = jarr.toString(2);
		} catch (JSONException e) {
			logger.error("There was a problem converting to JSON",e);
		}
		return retVal;
	}

	/**
	 * For internal verification and unit testing only.
	 * @param toolStream
	 * @return
	 */
	public boolean isEquivalent(ToolStream toolStream) {
		List<org.lubick.localHub.ToolStream.ToolUsage> otherList = toolStream.getAsList();
		if (numberOfToolUses() != otherList.size())
			return false;
		for(int i = 0;i< numberOfToolUses(); i++)
		{
			IdealizedToolStream.ToolUsage thisToolUse = listOfToolUsages.get(i);
			org.lubick.localHub.ToolStream.ToolUsage otherToolUse = otherList.get(i);
			if (!thisToolUse.isEquivalent(otherToolUse))
			{
				return false;
			}
		}
		return true;
	}

	public void addToolUsage(String toolString, String classString, String keyPressesString, Date timestamp, int duration) {
		ToolUsage tu = new ToolUsage(toolString, classString, keyPressesString, timestamp, duration);
		listOfToolUsages.add(tu);
	}

	public void createAndAppendRandomTools(int numberOfToolsToEmulate) 
	{
		// TODO Auto-generated method stub
		
	}

	public int numberOfToolUses() {
		return this.listOfToolUsages.size();
	}
	
	

	public List<ToolUsage> getAsList() {
		return new ArrayList<>(this.listOfToolUsages);
	}

public class ToolUsage {
		
		private String toolName, toolClass, keyPresses;
		private Date timeStamp;
		private int duration;
		
		public ToolUsage(String toolName, String toolClass, String keyPresses, Date timeStamp, int duration) 
		{
			this.toolName = toolName;
			this.toolClass = toolClass;
			this.keyPresses = keyPresses;
			this.timeStamp = timeStamp;
			this.duration = duration;
		}

		public JSONObject toJSONObject() throws JSONException 
		{
			JSONObject jobj = new JSONObject();
			
			jobj.put(ToolStream.TOOL_NAME, toolName);
			jobj.put(ToolStream.TOOL_CLASS, toolClass);
			jobj.put(ToolStream.TOOL_KEY_PRESSES, keyPresses);
			jobj.put(ToolStream.TOOL_TIMESTAMP, timeStamp.getTime());
			jobj.put(ToolStream.TOOL_DURATION, duration);
			
			return jobj;
		}

		public boolean isEquivalent(org.lubick.localHub.ToolStream.ToolUsage otherToolUse) 
		{
			return 	this.toolName.equals(otherToolUse.getToolName()) &&
					this.toolClass.equals(otherToolUse.getToolClass()) &&
					this.keyPresses.equals(otherToolUse.getToolKeyPresses()) &&
					this.timeStamp.equals(otherToolUse.getTimeStamp()) &&
					this.duration == otherToolUse.getDuration();
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

	}
}
