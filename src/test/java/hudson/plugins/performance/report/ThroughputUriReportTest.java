package hudson.plugins.performance.report;

import hudson.plugins.performance.model.HttpPerformanceReportSample;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class ThroughputUriReportTest {

    private UriReport uriReport = new UriReport("f", "x");

    private ThroughputUriReport throughputUriReport = new ThroughputUriReport(uriReport);

    @Test
    public void shouldReturnZeroIfNoHttpSamples() {
        assertEquals(0, throughputUriReport.get());
    }

    @Test
    public void shouldReturnThroughputEvenIfOneHttpSample() {
        HttpPerformanceReportSample httpSample1 = new HttpPerformanceReportSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(1000);

        uriReport.addHttpSample(httpSample1);

        assertEquals(1, throughputUriReport.get());
    }

    @Test
    public void shouldReturnZeroWhenAllRequestsTookMoreSecond() {
        HttpPerformanceReportSample httpSample1 = new HttpPerformanceReportSample();
        httpSample1.setDate(new Date());
        httpSample1.setDuration(10000);

        uriReport.addHttpSample(httpSample1);

        assertEquals(0, throughputUriReport.get());
    }

    @Test
    public void shouldReturnCountOfRequestIfAllRequestsTookLessThanOneSecond() {
        HttpPerformanceReportSample httpSample1 = new HttpPerformanceReportSample();
        httpSample1.setDate(new Date());

        HttpPerformanceReportSample httpSample2 = new HttpPerformanceReportSample();
        httpSample2.setDate(new Date());

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        assertEquals(2, throughputUriReport.get());
    }

    @Test
    public void shouldCalculateThroughput1() {
        long time = System.currentTimeMillis();

        // 0 sec - first request  - 1 sec
        // 0 sec -                - 1 sec - second request - 2 sec
        // 0 sec - third request  - 1 sec
        // 0 sec - four request   - 1 sec
        // 0 sec - total 3        - 1 sec - total 1        - 2 sec
        // throughput (per second) 2

        HttpPerformanceReportSample httpSample1 = new HttpPerformanceReportSample();
        httpSample1.setDate(new Date(time));

        HttpPerformanceReportSample httpSample2 = new HttpPerformanceReportSample();
        httpSample2.setDate(new Date(time + 1000));

        HttpPerformanceReportSample httpSample3 = new HttpPerformanceReportSample();
        httpSample3.setDate(new Date(time));
        httpSample3.setDuration(500);

        HttpPerformanceReportSample httpSample4 = new HttpPerformanceReportSample();
        httpSample4.setDate(new Date(time));
        httpSample4.setDuration(10);

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);

        assertEquals(2, throughputUriReport.get());
    }

    @Test
    public void shouldCalculateThroughput2() {
        long time = System.currentTimeMillis();

        // 0 sec - start first request, start second r
        // 1 sec - finish first r, finish second r
        // 2 sec -
        // 3 sec - start 3 r
        // 4 sec -
        // 5 sec -
        // 6 sec - finish 3 r
        // throughput per second 1

        HttpPerformanceReportSample httpSample1 = new HttpPerformanceReportSample();
        httpSample1.setDate(new Date(time));
        httpSample1.setDuration(1000);

        HttpPerformanceReportSample httpSample2 = new HttpPerformanceReportSample();
        httpSample2.setDate(new Date(time));
        httpSample2.setDuration(1000);

        HttpPerformanceReportSample httpSample3 = new HttpPerformanceReportSample();
        httpSample3.setDate(new Date(time + 3000));
        httpSample3.setDuration(3000);

        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);
        uriReport.addHttpSample(httpSample3);

        assertEquals(0, throughputUriReport.get());
    }

}
