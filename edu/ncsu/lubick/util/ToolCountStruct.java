package edu.ncsu.lubick.util;

public class ToolCountStruct implements Comparable<ToolCountStruct> {

	public final String toolName;
	public final Integer guiToolCount;	
	public final Integer keyboardCount;
	
	public ToolCountStruct(String toolName, int guiCount, int keyboardCount)
	{
		this.toolName = toolName;
		this.guiToolCount = guiCount;
		this.keyboardCount = keyboardCount;
	}

	@Override
	public int compareTo(ToolCountStruct o)
	{
		int thisTotal = guiToolCount + keyboardCount;
		int otherTotal = o.guiToolCount + o.keyboardCount;
		
		
		if (thisTotal != otherTotal)
		{
			return otherTotal - thisTotal; // reversed (from a standard compare) so that Collections.sort 
											//sorts in descending order
		}
		//if tied, then sort by gui tool count
		if (this.guiToolCount.compareTo(o.guiToolCount) != 0)
		{
			return o.guiToolCount.compareTo(this.guiToolCount); // reversed so that Collections.sort sorts in
															// descending order
		}
		//if still tied, go alphabetically
		return o.toolName.compareTo(this.toolName);
	}
	
	

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((guiToolCount == null) ? 0 : guiToolCount.hashCode());
		result = prime * result + ((keyboardCount == null) ? 0 : keyboardCount.hashCode());
		result = prime * result + ((toolName == null) ? 0 : toolName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ToolCountStruct other = (ToolCountStruct) obj;
		if (guiToolCount == null)
		{
			if (other.guiToolCount != null)
				return false;
		}
		else if (!guiToolCount.equals(other.guiToolCount))
			return false;
		if (keyboardCount == null)
		{
			if (other.keyboardCount != null)
				return false;
		}
		else if (!keyboardCount.equals(other.keyboardCount))
			return false;
		if (toolName == null)
		{
			if (other.toolName != null)
				return false;
		}
		else if (!toolName.equals(other.toolName))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "ToolCountStruct [toolCount=" + guiToolCount + ", toolName=" + toolName + "]";
	}

}
