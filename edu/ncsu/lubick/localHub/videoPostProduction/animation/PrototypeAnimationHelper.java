package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Used in development only to quickly prototype what the animations look like
 * @author KevinLubick
 *
 */
class PrototypeAnimationHelper extends JPanel implements KeyListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 701852946292219382L;

	private Set<Integer> activatedAnimations = new HashSet<>();
	private transient AnimatedKeypressMaker animationSource = new AnimatedTextAndKeyboardMaker();

	public PrototypeAnimationHelper() throws IOException
	{
	}

	
	public static void main(String[] args) throws IOException
	{
		JFrame outerFrame = new JFrame("test");
		outerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		outerFrame.setBounds(0, 0, 560, 300);

		PrototypeAnimationHelper innerPanel = new PrototypeAnimationHelper();
		innerPanel.setSize(800, 600);
		outerFrame.add(innerPanel);
		outerFrame.addKeyListener(innerPanel);
		outerFrame.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				System.out.println(e);
			}
		});
		outerFrame.setVisible(true);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		int[] keycodes = new int[this.activatedAnimations.size()];
		int index = 0;
		for (Integer thisInt : this.activatedAnimations)
		{
			keycodes[index] = thisInt;
			index++;
		}
		BufferedImage animatedImage = animationSource.makeAnimationForKeyCodes(keycodes);
		g.drawImage(animatedImage, 0, 0, null);
		// for (AnimatedKeyPress animations : activatedAnimations.values())
		// {
		// animations.drawAnimatedSegment(g, activatedKeyboard);
		// }
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		System.out.println("Pressed: " + e);

		activatedAnimations.add(e.getKeyCode());
		repaint();

	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		System.out.println("Released: " + e);
		activatedAnimations.remove(e.getKeyCode());
		repaint();
	}

	@Override
	public void keyTyped(KeyEvent e)
	{

	}

}
