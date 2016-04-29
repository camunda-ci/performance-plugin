package hudson.plugins.performance.parser;

import hudson.model.FreeStyleBuild;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

public class JmeterSummarizerParserTest extends HudsonTestCase {

    @Test
    public void testParse() throws Exception {
        JMeterSummarizerParser jmeterSummarizerParser = new JMeterSummarizerParser("summary.log", null);
        URL resource = getClass().getResource("/summary.log");
        File summaryLogFile = new File(resource.toURI());
        jmeterSummarizerParser.parse(new FreeStyleBuild(createFreeStyleProject()), Arrays.asList(summaryLogFile), createTaskListener());
    }

}
