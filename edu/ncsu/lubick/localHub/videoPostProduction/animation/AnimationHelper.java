package edu.ncsu.lubick.localHub.videoPostProduction.animation;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class AnimationHelper extends JPanel implements MouseMotionListener, KeyListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 701852946292219382L;
	private BufferedImage unActivatedKeyboard = null;
	private BufferedImage activatedKeyboard = null;
	
	
	
	private AnimatedKeyPress currentRectangle = AnimatedKeyPressFactory.getOffScreen();
	
	
	public AnimationHelper() throws IOException {
		File unactivatedKeyboardPath = new File("bin/imageAssets/QWERTY_keyboard_small.png");
		System.out.println(unactivatedKeyboardPath.getAbsolutePath());
		unActivatedKeyboard = ImageIO.read(unactivatedKeyboardPath);
		
		File activatedKeyboardPath = new File("bin/imageAssets/QWERTY_keyboard_pressed_small.png");
		activatedKeyboard = ImageIO.read(activatedKeyboardPath);
	}
	
	public static void main(String[] args) throws IOException {
		JFrame outerFrame = new JFrame("test");
		outerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		outerFrame.setBounds(0, 0, 540, 190);
		
		AnimationHelper innerPanel = new AnimationHelper();
		innerPanel.setSize(800,600);
		outerFrame.add(innerPanel);
		//innerPanel.addMouseMotionListener(innerPanel);
		outerFrame.addKeyListener(innerPanel);
		outerFrame.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println(e);
			}
		});
		outerFrame.setVisible(true);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(unActivatedKeyboard, 0, 0, null);
		currentRectangle.drawAnimatedSegment(g, activatedKeyboard);
//		g.drawImage(activatedKeyboard, currentRectangle.x, currentRectangle.y,
//				currentRectangle.x + currentRectangle.width, currentRectangle.y + currentRectangle.height,
//				currentRectangle.x, currentRectangle.y,
//				currentRectangle.x + currentRectangle.width, currentRectangle.y + currentRectangle.height, null);
	}

	@Override
	public void mouseDragged(MouseEvent e) {mouseMoved(e);}

	@Override
	public void mouseMoved(MouseEvent e) {
		//currentRectangle.x = e.getX();
		//currentRectangle.y = e.getY();
		repaint();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println("Pressed: "+e);
//		Point rowCol = getRowColForKeyPress(e);
//		System.out.println(rowCol);
//		AnimatedKeyPress adjustedRectToShow = createRecForRowCol(rowCol);
//		if (adjustedRectToShow == null)
//		{
//			return;
//		}
//		this.currentRectangle = adjustedRectToShow;
		this.currentRectangle = AnimatedKeyPressFactory.makeAnimatedKeyPress(e);
		repaint();
		
	}



	@Override
	public void keyReleased(KeyEvent e) {
		System.out.println("Released: "+e);
		currentRectangle = AnimatedKeyPressFactory.getOffScreen();
		repaint();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
	}
	
	
}
