package game.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SaveGameUtility {
  /**
   * reads a level-state from disc.
   * 
   * @param filePath the file holding the data
   * @return a 2-dimensional char-array holding the parsed data
   * @throws IOException if an error occurs
   */
  public static char[][] readFromFile(Path filePath) throws IOException {
    if (filePath == null) {
      return null;
    }

    return Files.lines(filePath).map(s -> s.toCharArray()).toArray(char[][]::new);
  }

  /**
   * saves the current level-state to a file on disc.
   * 
   * @param filePath where to save the file
   * @param data the actual level-state as 2-dimensional char-array
   * @throws IOException if an error occurs
   */
  public static void saveToFile(Path filePath, char[][] data) throws IOException {
    if (filePath == null || data == null) {
      throw new IllegalStateException("Parameters can not be null!");
    }

    try (BufferedWriter out = Files.newBufferedWriter(filePath)) {
      for (char[] c : data) {
        out.write(c);
        out.newLine();
      }
    }
  }
}
