package com.main;



import com.gameEngine.GameLoop;
import com.helpers.KeyboardListener;

import javax.swing.*;

public class MainWindow extends JFrame {

	public static void main(String[] args) {
		new MainWindow();
	}

	public MainWindow() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize( 1000, 600);
		setExtendedState(MAXIMIZED_BOTH);
		setTitle("Просто название окна и ничего лишнего. В сумме 10 слов");
		KeyboardListener keyb = new KeyboardListener();
		addKeyListener(keyb);
		add(new GameLoop(keyb));
		setVisible(true);
	}

}

