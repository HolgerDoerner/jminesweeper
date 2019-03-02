package game.gui;

import game.Game;
import java.awt.Component;
import java.awt.GridLayout;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class GameDialogs {
  private GameDialogs() {}

  /**
   * shows a dialog for starting a new game with custom settings.
   * 
   * @param parent the window calling this dialog
   * @return the values entered by the user
   */
  public static int[] showNewGameDialog(Component parent) {
    final JLabel lblSizeY = new JLabel("Size Y:");
    final JLabel lblSizeX = new JLabel("Size X:");
    final JLabel lblNumBombs = new JLabel("Number of Bombs:");
    final JTextField txtSizeY = new JTextField("8");
    final JTextField txtSizeX = new JTextField("8");
    final JTextField txtNumBombs = new JTextField("10");
    final JPanel panel = new JPanel();

    panel.setLayout(new GridLayout(3, 2));

    panel.add(lblSizeY);
    panel.add(txtSizeY);
    panel.add(lblSizeX);
    panel.add(txtSizeX);
    panel.add(lblNumBombs);
    panel.add(txtNumBombs);

    int userChoice = JOptionPane.showConfirmDialog(parent, panel, "jMinesweeper - New Game",
        JOptionPane.OK_OPTION);

    switch (userChoice) {
      case JOptionPane.OK_OPTION:
        int y = 0;
        int x = 0;
        int b = 0;

        try {
          y = Integer.parseInt(txtSizeY.getText());
          x = Integer.parseInt(txtSizeX.getText());
          b = Integer.parseInt(txtNumBombs.getText());
        } catch (NumberFormatException e) {
          if (Game.DEBUG) {
            e.printStackTrace();
          }
          return null;
        }

        return new int[] {y, x, b};

      default:
        return null;
    }
  }

  /**
   * shows a dialog which lets the player choose a file from disc holding a saved level-state.
   * 
   * @param parent the window calling this dialog
   * @return the java.nio.Path to the selected file
   */
  public static Path showLoadGameDialog(Component parent) {
    JFileChooser fileChooser = new JFileChooser();
    int choice = fileChooser.showOpenDialog(parent);

    if (choice == 0) {
      return fileChooser.getSelectedFile().toPath();
    } else {
      return null;
    }
  }

  /**
   * shows a dialog for saving the current level-state to disc.
   * 
   * @param parent the window calling this dialog
   * @return the java.nio.Path to the file
   */
  public static Path showSaveGameDialog(Component parent) {
    JFileChooser fileChooser = new JFileChooser();
    int userChoice = fileChooser.showSaveDialog(parent);

    if (userChoice == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile().toPath();
    } else {
      return null;
    }
  }
}
