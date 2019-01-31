package game;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public final class Level {
	private static char[][] level;
	
	private Level() {}
	
	public static final char[][] newLevel(int x, int y, int numBombs) {
		if (x <= 0 || y <= 0 || numBombs < 0)
			throw new IllegalArgumentException("Values can't be 0 or less!");
		
		int fieldsize = x * y;
		
		if (fieldsize <= numBombs)
			throw new IllegalStateException("Number of Bombs musst be lower than size of level!");
		
		@SuppressWarnings("unused")
		int untouchedCountdown = 0;
		
		level = new char[x][y];
		
		for (char[] c : level) {
			Arrays.fill(c, GameConstants.UNTOUCHED);
		}
		
		int bombcount = 0;
		
		while (bombcount < numBombs) {
			for (int i = 0; i < x; i++) {
				for (int j = 0; j < y; j++) {
					if ((ThreadLocalRandom.current().nextInt(0, numBombs) % fieldsize == 0) && (bombcount < numBombs)
							&& level[i][j] != GameConstants.BOMB) {
						level[i][j] = GameConstants.BOMB;
						bombcount++;
					}
				}
			}
		}
		
		return level;
	}
	
	public static char[][] getLevel() {
		return level;
	}
}
