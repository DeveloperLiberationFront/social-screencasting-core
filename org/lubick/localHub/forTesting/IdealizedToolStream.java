package org.lubick.localHub.forTesting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	
	
	private List<ToolUsage> listOfToolUsages;

	public IdealizedToolStream() {
		listOfToolUsages = new ArrayList<>();
	}

	public static IdealizedToolStream generateRandomToolStream(int numberOfCommands) {
		// TODO Auto-generated method stub
		return new IdealizedToolStream();
	}

	public String toJSON() {
		// TODO Auto-generated method stub
		return "This was some text.  Aren't you so proud?";
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
			if (!thisToolUse.equals(otherToolUse))
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

	public void createAndAppendRandomTools(int numberOfToolsToEmulate) {
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
		
		public ToolUsage(String toolName, String toolClass, String keyPresses, Date timeStamp, int duration) {
			this.toolName = toolName;
			this.toolClass = toolClass;
			this.keyPresses = keyPresses;
			this.timeStamp = timeStamp;
			this.duration = duration;
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
