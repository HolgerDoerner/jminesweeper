package game.util;

public final class DebugView {
  private DebugView() {}

  /**
   * prints current level-layout to console for debugging.
   * 
   * @param level current level as 2-dimensional char-array
   */
  public static final void printLevel(final char[][] level) {
    if (level == null) {
      throw new IllegalArgumentException("Argument can't be NULL!");
    }

    for (int i = 0; i < level.length; i++) {
      for (int j = 0; j < level[i].length; j++) {
        System.out.print(level[i][j] + " ");
      }

      System.out.println();
    }
  }
}
