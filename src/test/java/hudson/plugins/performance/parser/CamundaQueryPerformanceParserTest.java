package hudson.plugins.performance.parser;

import hudson.plugins.performance.report.PerformanceReport;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CamundaQueryPerformanceParserTest {

  @Test
  public void testParsing() throws Exception {
    // Setup fixture.
    final CamundaQueryPerformanceParser parser = new CamundaQueryPerformanceParser(null);
    final File reportFile = new File( getClass().getResource("/camundaqueryperformance/HistoryPerformanceTest.testQuery.json").toURI() );

    // Execute system under test.
    final PerformanceReport result = parser.parse(reportFile);

    // Verify results.
    assertNotNull(result);
    assertEquals("The source file contains three samples. These should all have been added to the performance report.",
        1, result.size());
  }

}
