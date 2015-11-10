package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AnimatedTextAndKeyboardMaker extends AbstractKeypressAnimationMaker {

	public AnimatedTextAndKeyboardMaker() throws IOException
	{
		super();
	}

	@Override
	public BufferedImage makeNewAnimationForKeyPresses(int[] keycodes, String toolKeyPresses)
	{
		BufferedImage image = super.makeAnimatedKeyboardForKeycodes(keycodes);

		if (keycodes.length == 0)
		{
			return image;
		}

		return addTextToImage(image, unActivatedKeyboard.getHeight(), toolKeyPresses);
	}

	@Override
	public BufferedImage makeUnactivatedAnimation()
	{
		int width = unActivatedKeyboard.getWidth();
		int height = unActivatedKeyboard.getHeight();
		BufferedImage img = new BufferedImage(width, height + EXTRA_HEIGHT, BufferedImage.TYPE_INT_ARGB);

		Graphics graphics = img.getGraphics();

		graphics.setColor(Color.white);
		graphics.fillRect(0, 0, width, height + EXTRA_HEIGHT);
		copyFromImageToGraphics(graphics, unActivatedKeyboard, 0, 0, width, height);
		
		graphics.dispose();
		return img;
	}

	@Override
	public String getAnimationTypeName()
	{
		return "image_text";
	}

}
