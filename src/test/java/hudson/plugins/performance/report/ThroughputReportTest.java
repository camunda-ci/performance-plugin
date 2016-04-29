package hudson.plugins.performance.report;

import hudson.plugins.performance.model.HttpPerformanceReportSample;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputReportTest {

    private PerformanceReport performanceReport = new PerformanceReport();

    private ThroughputReport throughputReport = new ThroughputReport(performanceReport);

    @Test
    public void shouldReturnZeroIfNoUri() {
        assertEquals(0, throughputReport.get());
    }

    @Test
    public void shouldSummarizeThroughputByDifferentUri() {
        HttpPerformanceReportSample httpSample1 = new HttpPerformanceReportSample();
        httpSample1.setDate(new Date());

        UriReport uriReport1 = new UriReport("f", "url1");
        uriReport1.addHttpSample(httpSample1);

        HttpPerformanceReportSample httpSample2 = new HttpPerformanceReportSample();
        httpSample2.setDate(new Date());

        UriReport uriReport2 = new UriReport("f", "url2");
        uriReport2.addHttpSample(httpSample2);

        performanceReport.getUriReportMap().put(uriReport1.getUri(), uriReport1);
        performanceReport.getUriReportMap().put(uriReport2.getUri(), uriReport2);

        assertEquals(2, throughputReport.get());
    }

}
