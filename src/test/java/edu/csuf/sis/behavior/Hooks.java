package edu.csuf.sis.behavior;

import edu.csuf.sis.support.TestData;
import io.cucumber.java.Before;

public class Hooks {

    private final TestData testData;
    private final World world;

    public Hooks(TestData testData, World world) {
        this.testData = testData;
        this.world = world;
    }

    @Before
    public void startFromAnEmptyDatabase() {
        testData.reset();
        world.setRegistrarAccountId(testData.registrar());
    }
}
