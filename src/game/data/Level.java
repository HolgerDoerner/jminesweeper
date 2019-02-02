package game.data;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import game.Game;

public final class Level {
	
	private Level() {}
	
	public static final char[][] newLevel(int y, int x, int numBombs) {
		if (y <= 0 || x <= 0 || numBombs < 0)
			throw new IllegalArgumentException("Values can't be 0 or less!");
		
		int fieldsize = y * x;
		
		if (fieldsize <= numBombs)
			throw new IllegalStateException("Number of Bombs musst be lower than size of level!");
		
		@SuppressWarnings("unused")
		int untouchedCountdown = 0;
		
		char[][] level = new char[y][x];
		
		for (char[] c : level) {
			Arrays.fill(c, Game.UNTOUCHED);
		}
		
		int bombcount = 0;
		
		while (bombcount < numBombs) {
			for (int i = 0; i < y; i++) {
				for (int j = 0; j < x; j++) {
					if ((ThreadLocalRandom.current().nextInt(0, numBombs) % fieldsize == 0) && (bombcount < numBombs)
							&& level[i][j] != Game.BOMB) {
						level[i][j] = Game.BOMB;
						bombcount++;
					}
				}
			}
		}
		
		return level;
	}
}
