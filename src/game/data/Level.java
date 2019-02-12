package game.data;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import game.Game;

/**
 * <p>
 * an interface describing a level for the game.
 * </p>
 * 
 * <p>
 * it defines two static factory-methods wich both return concrete instances of the interface: 
 * <ul>
 * <li>{@code generateNewLevel(int, int, int)}</li>
 * <li>{@code fromExistingData(char[][])}</li>
 * </ul>
 * </p>
 * 
 * @author Holger Dörner
 *
 */
public interface Level {
	/**
	 * returns the value from the given position in the level.
	 * 
	 * @param positionY the vertical position
	 * @param positionX the horizontal position
	 * @return the the value stored at the given position
	 */
	public char get(final int positionY, final int positionX);
	
	/**
	 * sets a new value at the given position in the level.
	 *
	 * @param positionY the vertical position
	 * @param positionX the horizontal position
	 * @param value the new value to be stored at the given position
	 */
	public void set(final int positionY, final int positionX, final char value);
	
	/**
	 * increments the value at the given position by 1.
	 *
	 * @param positionY the vertical position
	 * @param positionX the horizontal position
	 */
	public void increment(final int positionY, final int positionX);
	
	/**
	 * returns the vertical size of the level.
	 *
	 * @returnthe vertical size
	 */
	public int getSizeY();
	
	/**
	 * returns the horizontal size of the level.
	 *
	 * @return the horizontal size
	 */
	public int getSizeX();
	
	/**
	 * returns the raw data of the level as a 2-dimensional-array.
	 *
	 * @return the raw level data
	 */
	public char[][] getLevelData();
	
	/**
	 * returns a instance of {@code game.data.Level} based on existing data.
	 *
	 * @param levelData the raw level-data
	 * @return an instance of {@code game.data.Level}
	 */
	public static Level fromExistingData(char[][] levelData) {
		if (levelData == null)
			throw new IllegalArgumentException("Argument can't be NULL!");

		return new Level() {
			private final char[][] _level_ = levelData.clone();

			@Override
			public void set(int positionY, int positionX, char value) {
				_level_[positionY][positionX] = value;
			}

			@Override
			public int getSizeY() {
				return _level_.length;
			}

			@Override
			public int getSizeX() {
				return _level_[0].length;
			}

			@Override
			public char[][] getLevelData() {
				return _level_;
			}

			@Override
			public char get(int positionY, int positionX) {
				return _level_[positionY][positionX];
			}

			@Override
			public void increment(int positionY, int positionX) {
				_level_[positionY][positionX]++;
			}
		};
	}

	/**
	 * generates a random level based on initial values.
	 *
	 * @param y the vertical size of the new level
	 * @param x the horizontal size of the new level
	 * @param numBombs the number of bombs in the new level
	 * @return a randomized instance of {@code game.data.Level}
	 * @throws IllegalArgumentException if argument(s) are <=0
	 */
	public static Level generateNew(int y, int x, int numBombs) {
		if (y <= 0 | x <= 0 | numBombs <= 0)
			throw new IllegalArgumentException("Values can't be 0 or less!");

		int fieldsize = y * x;

		if (fieldsize <= numBombs)
			throw new IllegalStateException("Number of Bombs musst be lower than size of level!");

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

		return new Level() {
			private final char[][] _level_ = level.clone();

			@Override
			public void set(int positionY, int positionX, char value) {
				_level_[positionY][positionX] = value;
			}

			@Override
			public int getSizeY() {
				return _level_.length;
			}

			@Override
			public int getSizeX() {
				return _level_[0].length;
			}

			@Override
			public char[][] getLevelData() {
				return _level_;
			}

			@Override
			public char get(int positionY, int positionX) {
				return _level_[positionY][positionX];
			}

			@Override
			public void increment(int positionY, int positionX) {
				_level_[positionY][positionX]++;
			}
		};
	}
}
