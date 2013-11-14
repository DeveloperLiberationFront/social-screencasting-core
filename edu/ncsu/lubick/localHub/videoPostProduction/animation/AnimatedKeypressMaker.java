package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.image.BufferedImage;

public interface AnimatedKeypressMaker {

	BufferedImage makeAnimationForKeyCodes(int[] keycodes);

	BufferedImage makeUnactivatedAnimation();

	void setCurrentKeyPresses(String toolKeyPresses);

	String getAnimationTypeName();

}
