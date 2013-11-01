package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.image.BufferedImage;

public interface KeypressAnimationMaker {

	BufferedImage makeAnimationForKeyCodes(int[] keycodes);

	BufferedImage makeUnactivatedAnimation();

	void setCurrentKeyPresses(String toolKeyPresses);

}
