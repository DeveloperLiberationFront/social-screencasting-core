package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AnimatedTextAndKeyboardMaker extends AnimatedKeyboardMaker {

	private static final int EXTRA_HEIGHT = 50;

	public AnimatedTextAndKeyboardMaker() throws IOException
	{
		super();
	}
	
	@Override
	public BufferedImage makeAnimationForKeyCodes(int[] keycodes)
	{
		BufferedImage image = super.makeAnimationForKeyCodes(keycodes);
		
		//g = image.createGraphics();
		//g.drawText...
		return image;
	}

	@Override
	public BufferedImage makeUnactivatedAnimation()
	{
		int width = unActivatedKeyboard.getWidth();
		int height = unActivatedKeyboard.getHeight();
		BufferedImage img = new BufferedImage(width, height+EXTRA_HEIGHT, BufferedImage.TYPE_INT_ARGB);

		Graphics graphics = img.getGraphics();
		
		graphics.setColor(Color.white);
		graphics.fillRect(0, 0, width, height+EXTRA_HEIGHT);
		copyFromImageToGraphics(graphics, unActivatedKeyboard, 0, 0, width, height);
		return img;
	}
}
