package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public abstract class OversizedAnimatedKey implements AnimatedKeyPress {

	protected abstract Rectangle getRegion();
	
	@Override
	public void drawAnimatedSegment(Graphics g, BufferedImage img) {
		g.drawImage(img, getRegion().x, getRegion().y, getRegion().x + getRegion().width,  getRegion().y + getRegion().height,
				getRegion().x, getRegion().y, getRegion().x + getRegion().width,  getRegion().y + getRegion().height, null);
	}

}

class TabLetterKey extends OversizedAnimatedKey 
{

	@Override
	protected Rectangle getRegion() {
		return new Rectangle(3, 67, 44, 24);
	}
	
}

class ShiftLetterKey extends OversizedAnimatedKey 
{

	@Override
	protected Rectangle getRegion() {
		return new Rectangle(3, 114, 61, 23);
	}
	
}

class ControlLetterKey extends OversizedAnimatedKey 
{

	@Override
	protected Rectangle getRegion() {
		return new Rectangle(3, 137, 37, 25);
	}
	
}

class AltLetterKey extends OversizedAnimatedKey 
{

	@Override
	protected Rectangle getRegion() {
		return new Rectangle(67, 137, 32, 25);
	}
	
}