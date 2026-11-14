package edu.csuf.sis.behavior;

import edu.csuf.sis.support.TestClockConfig;
import edu.csuf.sis.support.TestData;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Scenarios run against the real components and an in-memory database. */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@Import({TestClockConfig.class, TestData.class, World.class})
public class CucumberSpringConfiguration {
}
