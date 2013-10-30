package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Represents a block of same-sized keys, like letters (but also used for
 * numbers)
 * 
 * @author KevinLubick
 * 
 */
public abstract class AnimatedLetterKey implements AnimatedKeyPress
{

	final double widthOfSquare = 27.6;

	private int column;

	public AnimatedLetterKey(int column)
	{
		this.column = column;
	}

	protected abstract Point getBasePoint();

	protected abstract int getRowHeight();

	@Override
	public void drawAnimatedSegment(Graphics g, BufferedImage img)
	{

		int roundedWidthOfSquare = (int) Math.round(widthOfSquare);
		int offSetX = (int) Math.round(column * widthOfSquare + getBasePoint().x);

		g.drawImage(img, offSetX, getBasePoint().y, offSetX + roundedWidthOfSquare, getBasePoint().y + getRowHeight(),
				offSetX, getBasePoint().y, offSetX + roundedWidthOfSquare, getBasePoint().y + getRowHeight(), null);
	}

}

class QRowLetterKey extends AnimatedLetterKey
{
	final Point Q_START = new Point(47, 67);
	final int Q_HEIGHT = 24;

	public QRowLetterKey(int column)
	{
		super(column);
	}

	@Override
	protected Point getBasePoint()
	{
		return Q_START;
	}

	@Override
	protected int getRowHeight()
	{
		return Q_HEIGHT;
	}

}

class ARowLetterKey extends AnimatedLetterKey
{
	final Point A_START = new Point(56, 91);
	final int A_HEIGHT = 23;

	public ARowLetterKey(int column)
	{
		super(column);
	}

	@Override
	protected Point getBasePoint()
	{
		return A_START;
	}

	@Override
	protected int getRowHeight()
	{
		return A_HEIGHT;
	}

}

class ZRowLetterKey extends AnimatedLetterKey
{
	final Point Z_START = new Point(65, 114);
	final int Z_HEIGHT = 23;

	public ZRowLetterKey(int column)
	{
		super(column);
	}

	@Override
	protected Point getBasePoint()
	{
		return Z_START;
	}

	@Override
	protected int getRowHeight()
	{
		return Z_HEIGHT;
	}

}

class NumberRowLetterKey extends AnimatedLetterKey
{
	final Point BACKTICK_START = new Point(3, 43);
	final int BACKTICK_HEIGHT = 24;

	public NumberRowLetterKey(int column)
	{
		super(column);
	}

	@Override
	protected Point getBasePoint()
	{
		return BACKTICK_START;
	}

	@Override
	protected int getRowHeight()
	{
		return BACKTICK_HEIGHT;
	}

}
