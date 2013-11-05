package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AnimatedTextMaker extends AnimatedTextAndKeyboardMaker {

	public AnimatedTextMaker() throws IOException
	{
		super();

	}

	@Override
	public BufferedImage makeAnimationForKeyCodes(int[] keycodes)
	{
		return addTextToImage(makeUnactivatedAnimation(), 0);
	}

	@Override
	public BufferedImage makeUnactivatedAnimation()
	{
		int width = unActivatedKeyboard.getWidth();
		int height = EXTRA_HEIGHT;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.white);
		graphics.fillRect(0, 0, width, height);
		return image;
	}

}
