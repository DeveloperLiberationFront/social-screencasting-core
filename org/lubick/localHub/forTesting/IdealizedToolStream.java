package org.lubick.localHub.forTesting;

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
public class IdealizedToolStream {
	
	public IdealizedToolStream() {
		// TODO Auto-generated constructor stub
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
		// TODO Auto-generated method stub
		return false;
	}

}
