package game.data.tests;

import game.data.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;



public class LevelTest {
    @Test
    public void fromExistingDataTest() {
        char[][] testData = {{'O','@'}, {'@','O'}};

        Level level = Level.fromExistingData(testData);

        assertTrue(level.get(0,0) == 'O', "position 0,0 not correct");
        assertTrue(level.get(1,0) == '@', "position 1,0 not correct");
    }

    @Test
    public void generateNewTest() {
        Level level = Level.generateNew(5, 5, 2);
        assertTrue(level != null, "level is NULL!!");
        assertTrue(level.getSizeY() == 5, "level y != 5");
        assertTrue(level.getSizeX() == 5, "level x != 5");
    }
}
