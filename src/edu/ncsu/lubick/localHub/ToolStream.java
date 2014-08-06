package edu.ncsu.lubick.localHub;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ToolStream {


	

	private List<ToolUsage> listOfToolUsages;
	private Date timeStamp;
	private String pluginName;

	private static Logger logger = Logger.getLogger(ToolStream.class.getName());

	protected ToolStream()
	{
		listOfToolUsages = new ArrayList<>();
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
}
