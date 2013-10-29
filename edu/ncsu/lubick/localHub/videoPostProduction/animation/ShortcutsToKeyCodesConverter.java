package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import static java.awt.event.KeyEvent.*;

import java.util.ArrayList;

public class ShortcutsToKeyCodesConverter 
{
	//private static final String KEY_SEPERATORS = "[^,+]+([,+])[^,+]+";
	private static final String KEY_SEPERATORS = "[,+]";	//works in all but the cases like Ctrl+, (previous selection)
	private static final String REGEX_FUNCTION_KEYS = "F[0-9]+";
	private static final String SHIFT = "SHIFT";
	private static final String CONTROL = "CTRL";
	private static final String ALT = "ALT";
	
	
	
	public int[] convert(String keyCommandString) 
	{
		String uppercaseString = keyCommandString.toUpperCase();
		
		String[] individualKeys = splitOutCommands(uppercaseString);
		
		int[] retVal = new int[individualKeys.length];
		for(int i = 0;i<individualKeys.length;i++)
		{
			String trimmedCommand = individualKeys[i].trim();
			retVal[i] = makeKeyCode(trimmedCommand);
		}
		return retVal;
	}

	//Can't do a simple split because of things like CTRL+,
	//So, we will go one by one through each slot, checking to see if the command is the first thing
	private String[] splitOutCommands(String commands) 
	{
		ArrayList<String> buildUpCommands = new ArrayList<>();
		
		recursiveSplitOfCommands(buildUpCommands,commands);
		
		String[] retVal = new String[buildUpCommands.size()];
		return buildUpCommands.toArray(retVal);
	}

	private void recursiveSplitOfCommands(ArrayList<String> buildUpCommands, String commands) {
		if (commands.matches(KEY_SEPERATORS))
		{
			buildUpCommands.add(commands);
			return;
		}
		String[] things = commands.split(KEY_SEPERATORS,2);

		if (things.length == 0)
		{
			return;
		}
		buildUpCommands.add(things[0].trim());
		if (things.length == 1)
		{
			return; //nothing left to parse
		}
		recursiveSplitOfCommands(buildUpCommands, things[1].trim());
	}

	private int makeKeyCode(String command) 
	{
		if (command.length() == 1)
		{
			return handleSingleLetterOrNumber(command);
		}
		if (command.matches(REGEX_FUNCTION_KEYS))
		{
			return handleFunctionKey(command);
		}
		return handleSpecialKey(command);
	}

	//Handles things like control, shift, etc
	private int handleSpecialKey(String command) {
		switch (command) {
		case SHIFT:
			return VK_SHIFT;
		case CONTROL:
			return VK_CONTROL;	
		case ALT:
			return VK_ALT;
		default:
			break;
		}
		return -1;
	}

	private int handleFunctionKey(String command) {
		String numberString = command.substring(1);
		
		return VK_F1 + Integer.valueOf(numberString) - 1;
	}

	private int handleSingleLetterOrNumber(String command) {
		try
		{
			int numberForKeyCode = Integer.valueOf(command);
			return handleNumber(numberForKeyCode);
		}
		catch (NumberFormatException e)
		{
			return handleLetter(command.charAt(0));
		}

	}

	private int handleLetter(char c) 
	{
		return VK_A+c - 'A';
	}

	private int handleNumber(int numberForKeyCode) {
		return VK_0 + numberForKeyCode;
	}

	
}