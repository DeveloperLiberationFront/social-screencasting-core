package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class AnimatedKeyPressFactory 
{
	private static char[] firstRow = new char[]{'q','w','e','r','t','y','u','i','o','p','[',']'};
	private static char[] secondRow = new char[]{'a','s','d','f','g','h','j','k','l',';','\''};
	private static char[] thirdRow = new char[]{'z','x','c','v','b','n','m',',','.','/'};
	
	
	private static AnimatedKeyPress offscreen = new AnimatedKeyPress() {
		
		@Override
		public void drawAnimatedSegment(Graphics g, BufferedImage img) {}	//do nothing
	};

	
	private AnimatedKeyPressFactory() {}
	
	public static AnimatedKeyPress makeAnimatedKeyPress(KeyEvent ke)
	{
		if (getIndexInArray(firstRow, ke.getKeyChar()) != -1)
		{
			return new QRowLetterKey(getIndexInArray(firstRow, ke.getKeyChar()));
		}
		if (getIndexInArray(secondRow, ke.getKeyChar())!= -1)
		{
			return new ARowLetterKey(getIndexInArray(secondRow, ke.getKeyChar()));
		}
		if (getIndexInArray(thirdRow, ke.getKeyChar())!= -1)
		{
			return new ZRowLetterKey(getIndexInArray(thirdRow, ke.getKeyChar()));
		}
		
		return getOffScreen();
	}
	
	public static AnimatedKeyPress getOffScreen()
	{
		return offscreen;
	}
	
	private static int getIndexInArray(char[] array, char searchTerm)
	{
		for(int i = 0;i<array.length;i++)
		{
			if (array[i] == searchTerm)
			{
				return i;
			}
		}
		return -1;
	}
		
}
