package edu.ncsu.lubick.util;

public class ToolCountStruct implements Comparable<ToolCountStruct> {

	public Integer toolCount;
	public String toolName;
	
	public ToolCountStruct(String toolName, int count)
	{
		this.toolName = toolName;
		this.toolCount = count;
	}

	@Override
	public int compareTo(ToolCountStruct o)
	{
		if (this.toolCount.compareTo(o.toolCount) != 0)
		{
			return o.toolCount.compareTo(this.toolCount); // reversed so that Collections.sort sorts in
															// descending order
		}
		return o.toolName.compareTo(this.toolName);
	}
	
	@Override
	public int hashCode()
	{
		return (toolName + toolCount.toString()).hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ToolCountStruct)
		{
			return this.hashCode() == obj.hashCode();
		}
		return false;
	}

	@Override
	public String toString()
	{
		return "ToolCountStruct [toolCount=" + toolCount + ", toolName=" + toolName + "]";
	}

}
