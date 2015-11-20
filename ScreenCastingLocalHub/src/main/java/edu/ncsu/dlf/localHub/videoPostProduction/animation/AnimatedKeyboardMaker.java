package edu.ncsu.dlf.localHub.videoPostProduction.animation;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class AnimatedKeyboardMaker extends AbstractKeypressAnimationMaker {

	public AnimatedKeyboardMaker() throws IOException
	{
		super();
	}

	@Override
	public BufferedImage makeNewAnimationForKeyPresses(int[] keycodes, String toolKeyPresses)
	{
		return makeAnimatedKeyboardForKeycodes(keycodes);

	}

	@Override
	public BufferedImage makeUnactivatedAnimation()
	{
		int width = unActivatedKeyboard.getWidth();
		int height = unActivatedKeyboard.getHeight();
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		copyFromImageToGraphics(img.getGraphics(), unActivatedKeyboard, 0, 0, width, height);
		return img;
	}

	@Override
	public String getAnimationTypeName()
	{
		return "image";
	}

}
