package game.gui;

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
	
	public static int[] showNewGameDialog() {
		final JLabel lblSizeY = new JLabel("Size Y:");
		final JLabel lblSizeX = new JLabel("Size X:");
		final JLabel lblNumBombs = new JLabel("Number of Bombs:");
		final JTextField txtSizeY = new JTextField("10");
		final JTextField txtSizeX = new JTextField("10");
		final JTextField txtNumBombs = new JTextField("15");
		final JPanel panel = new JPanel();
		
		panel.setLayout(new GridLayout(3, 2));
		
		panel.add(lblSizeY);
		panel.add(txtSizeY);
		panel.add(lblSizeX);
		panel.add(txtSizeX);
		panel.add(lblNumBombs);
		panel.add(txtNumBombs);
		
		int choice = JOptionPane.showConfirmDialog(null, panel, "New Game", JOptionPane.OK_OPTION);
		
		if (choice == 0) {
			int y = 0, x = 0, b = 0;
			
			try {
				y = Integer.parseInt(txtSizeY.getText());
				x = Integer.parseInt(txtSizeX.getText());
				b = Integer.parseInt(txtNumBombs.getText());
			} catch (NumberFormatException e) {
				if (Game.DEBUG)
					e.printStackTrace();
			}
			
			return new int[] { y, x, b };
		} else {
			return null;
		}
		
	}
	
	public static Path showLoadGameDialog() {
		JFileChooser fileChooser = new JFileChooser();
		int choice = fileChooser.showOpenDialog(null);
		
		if (choice == 0) {
			return fileChooser.getSelectedFile().toPath();
		} else {			
			return null;
		}
	}
	
}
