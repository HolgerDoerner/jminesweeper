package game.gui;

import java.awt.Component;
import java.awt.GridLayout;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import game.Game;

public final class GameDialogs {
	private GameDialogs() {
	}
	
	public static int[] showNewGameDialog(Component parent) {
		final JLabel lblSizeY = new JLabel("Size Y:");
		final JLabel lblSizeX = new JLabel("Size X:");
		final JLabel lblNumBombs = new JLabel("Number of Bombs:");
		final JTextField txtSizeY = new JTextField("9");
		final JTextField txtSizeX = new JTextField("9");
		final JTextField txtNumBombs = new JTextField("10");
		final JPanel panel = new JPanel();
		
		panel.setLayout(new GridLayout(3, 2));
		
		panel.add(lblSizeY);
		panel.add(txtSizeY);
		panel.add(lblSizeX);
		panel.add(txtSizeX);
		panel.add(lblNumBombs);
		panel.add(txtNumBombs);
		
		int userChoice = JOptionPane.showConfirmDialog(parent, panel, "jMinesweeper - New Game", JOptionPane.OK_OPTION);
		
		switch (userChoice) {
			case JOptionPane.OK_OPTION:
				int y = 0, x = 0, b = 0;
				
				try {
					y = Integer.parseInt(txtSizeY.getText());
					x = Integer.parseInt(txtSizeX.getText());
					b = Integer.parseInt(txtNumBombs.getText());
				} catch (NumberFormatException e) {
					if (Game.DEBUG)
						e.printStackTrace();
					return null;
				}
				
				return new int[] { y, x, b };
				
			default:
				return null;
		}		
	}
	
	public static Path showLoadGameDialog(Component parent) {
		JFileChooser fileChooser = new JFileChooser();
		int choice = fileChooser.showOpenDialog(parent);
		
		if (choice == 0) {
			return fileChooser.getSelectedFile().toPath();
		} else {			
			return null;
		}
	}
	
	public static Path showSaveGameDialog(Component parent) {
		JFileChooser fileChooser = new JFileChooser();
		int userChoice = fileChooser.showSaveDialog(parent);
		
		if (userChoice == JFileChooser.APPROVE_OPTION)
			return fileChooser.getSelectedFile().toPath();
		else
			return null;
	}
	
	
	public static void showDefeatDialog(Component parent) {
		JOptionPane.showMessageDialog(parent, "Dude, you had ONE job...", "GAME OVER", JOptionPane.ERROR_MESSAGE);
	}
	
	public static void showVictoryDialog(Component parent) {
		JOptionPane.showMessageDialog(parent, "You have WON this level !!!", "VICTORY !!!",
				JOptionPane.INFORMATION_MESSAGE);
	}
}
