package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import static java.awt.event.KeyEvent.*;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class AnimatedKeyPressFactory
{
	private static int[] functionRow = new int[] { VK_ESCAPE, VK_F1, VK_F2, VK_F3, VK_F4, VK_F5, VK_F6, VK_F7, VK_F8, VK_F9, VK_F10, VK_F11, VK_F12,
			VK_PRINTSCREEN, VK_SCROLL_LOCK, VK_PAUSE };
	private static int[] numberRow = new int[] { VK_BACK_QUOTE, VK_1, VK_2, VK_3, VK_4, VK_5, VK_6, VK_7, VK_8, VK_9, VK_0, VK_MINUS, VK_EQUALS, VK_BACK_SLASH,
			VK_BACK_SPACE };
	private static int[] qwertyRow = new int[] { VK_Q, VK_W, VK_E, VK_R, VK_T, VK_Y, VK_U, VK_I, VK_O, VK_P, VK_OPEN_BRACKET, VK_CLOSE_BRACKET };
	private static int[] asdfRow = new int[] { VK_A, VK_S, VK_D, VK_F, VK_G, VK_H, VK_J, VK_K, VK_L, VK_SEMICOLON, VK_QUOTE };
	private static int[] zxcvRow = new int[] { VK_Z, VK_X, VK_C, VK_V, VK_B, VK_N, VK_M, VK_COMMA, VK_PERIOD, VK_SLASH };

	private static AnimatedKeyPress offscreen = new AnimatedKeyPress() {

		@Override
		public void drawAnimatedSegment(Graphics g, BufferedImage img)
		{
		} // do nothing
	};

	private AnimatedKeyPressFactory()
	{
	}

	public static AnimatedKeyPress makeAnimatedKeyPress(int keyCode)
	{
		if (getIndexInArray(qwertyRow, keyCode) != -1)
		{
			return new QRowLetterKey(getIndexInArray(qwertyRow, keyCode));
		}
		if (getIndexInArray(asdfRow, keyCode) != -1)
		{
			return new ARowLetterKey(getIndexInArray(asdfRow, keyCode));
		}
		if (getIndexInArray(zxcvRow, keyCode) != -1)
		{
			return new ZRowLetterKey(getIndexInArray(zxcvRow, keyCode));
		}
		if (getIndexInArray(numberRow, keyCode) != -1)
		{
			return new NumberRowLetterKey(getIndexInArray(numberRow, keyCode));
		}
		if (getIndexInArray(functionRow, keyCode) != -1)
		{
			return new FunctionRowLetterKey(getIndexInArray(functionRow, keyCode));
		}
		if (keyCode == VK_TAB) // java doesn't seem to register this, so change
								// it to VK_T for debugging
		{
			return new TabLetterKey();
		}
		if (keyCode == VK_SHIFT)
		{
			return new ShiftLetterKey();
		}
		if (keyCode == VK_CONTROL)
		{
			return new ControlLetterKey();
		}
		if (keyCode == VK_ALT)
		{
			return new AltLetterKey();
		}
		if (keyCode == VK_ENTER)
		{
			return new EnterLetterKey();
		}

		return getOffScreen();
	}

	public static AnimatedKeyPress makeAnimatedKeyPress(KeyEvent ke)
	{
		int keyCode = ke.getKeyCode();
		return makeAnimatedKeyPress(keyCode);
	}

	public static AnimatedKeyPress getOffScreen()
	{
		return offscreen;
	}

	private static int getIndexInArray(int[] array, int searchTerm)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == searchTerm)
			{
				return i;
			}
		}
		return -1;
	}

}
