package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import static edu.ncsu.lubick.localHub.videoPostProduction.animation.AbstractKeypressAnimationMaker.*;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public abstract class OversizedAnimatedKey implements KeyPressAnimation {

	protected abstract Rectangle getRegion();

	@Override
	public void drawAnimatedSegment(Graphics g, BufferedImage img)
	{
		copyFromImageToGraphics(g, img, getRegion().x, getRegion().y,
				getRegion().x + getRegion().width, getRegion().y + getRegion().height);
	}

}

class TabLetterKey extends OversizedAnimatedKey
{

	@Override
	protected Rectangle getRegion()
	{
		return new Rectangle(3, 67, 44, 24);
	}

}

class ShiftLetterKey extends OversizedAnimatedKey
{

	@Override
	protected Rectangle getRegion()
	{
		return new Rectangle(3, 114, 61, 23);
	}

}

class ControlLetterKey extends OversizedAnimatedKey
{

	@Override
	protected Rectangle getRegion()
	{
		return new Rectangle(3, 137, 37, 25);
	}

}

class AltLetterKey extends OversizedAnimatedKey
{

	@Override
	protected Rectangle getRegion()
	{
		return new Rectangle(67, 137, 32, 25);
	}

}

class EnterLetterKey extends OversizedAnimatedKey
{
	// draws with two rectangle regions
	private Rectangle firstRectangle = new Rectangle(358, 91, 59, 24);
	private Rectangle secondRectangle = new Rectangle(379, 67, 38, 26);

	@Override
	protected Rectangle getRegion()
	{
		return firstRectangle;
	}

	@Override
	public void drawAnimatedSegment(Graphics g, BufferedImage img)
	{
		super.drawAnimatedSegment(g, img); // draws first rectangle

		// draw second rectangle
		AnimatedKeyboardMaker.copyFromImageToGraphics(g, img, secondRectangle.x, secondRectangle.y, secondRectangle.x + secondRectangle.width,
				secondRectangle.y + secondRectangle.height);
	}

}
