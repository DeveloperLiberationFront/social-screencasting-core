package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

public class FunctionRowLetterKey implements KeyPressAnimation
{
	final Point ESC_START = new Point(3, 5);
	final int ESC_HEIGHT = 25;

	final double widthOfSquare = 27.6;

	private int column;

	public FunctionRowLetterKey(int column)
	{
		this.column = column;
	}

	@Override
	public void drawAnimatedSegment(Graphics g, BufferedImage img)
	{
		int roundedWidthOfSquare = (int) Math.round(widthOfSquare);
		int offSetX = (int) Math.round(column * widthOfSquare + ESC_START.x);
		offSetX += getSpecialOffset();

		g.drawImage(img, offSetX, ESC_START.y, offSetX + roundedWidthOfSquare, ESC_START.y + ESC_HEIGHT,
				offSetX, ESC_START.y, offSetX + roundedWidthOfSquare, ESC_START.y + ESC_HEIGHT, null);
	}

	private int getSpecialOffset()
	{
		int offset = 0;

		if (this.column >= 1)
		{
			offset += 28;
		}
		if (this.column >= 5)
		{
			offset += 14;
		}
		if (this.column >= 9)
		{
			offset += 14;
		}
		if (this.column >= 13)
		{
			offset += 28;
		}

		return offset;
	}

}
