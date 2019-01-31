package game;

import java.awt.GridLayout;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

final class GameDialogs {
	private GameDialogs() {
	}
	
	static int[] showNewGameDialog() {
		final JLabel lblSizeX = new JLabel("Size X:");
		final JLabel lblSizeY = new JLabel("Size Y:");
		final JLabel lblNumBombs = new JLabel("Number of Bombs:");
		final JTextField txtSizeX = new JTextField("10");
		final JTextField txtSizeY = new JTextField("10");
		final JTextField txtNumBombs = new JTextField("30");
		final JPanel panel = new JPanel();
		
		panel.setLayout(new GridLayout(3, 2));
		
		panel.add(lblSizeX);
		panel.add(txtSizeX);
		panel.add(lblSizeY);
		panel.add(txtSizeY);
		panel.add(lblNumBombs);
		panel.add(txtNumBombs);
		
		int choice = JOptionPane.showConfirmDialog(null, panel, "New Game", JOptionPane.OK_OPTION);
		
		if (choice == 0) {
			int x = 0, y = 0, b = 0;
			
			try {
				x = Integer.parseInt(txtSizeX.getText());
				y = Integer.parseInt(txtSizeY.getText());
				b = Integer.parseInt(txtNumBombs.getText());
			} catch (NumberFormatException e) {
				if (GameConstants.DEBUG)
					e.printStackTrace();
			}
			
			return new int[] { x, y, b };
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
