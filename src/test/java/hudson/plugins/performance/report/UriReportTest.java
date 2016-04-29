package hudson.plugins.performance.report;

import hudson.plugins.performance.model.HttpPerformanceReportSample;
import hudson.plugins.performance.report.UriReport.Sample;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class UriReportTest {

	private static final long AVERAGE = 5;
	private static final long MIN = 0;
	private static final long MAX = 10;
	private UriReport uriReport;

	@Before
	public void setUp() {
		uriReport = new UriReport(null, null);
		HttpPerformanceReportSample httpSample1 = new HttpPerformanceReportSample();
		httpSample1.setDuration(MAX);
		Date date = new Date();
		httpSample1.setDate(date);
		httpSample1.setSuccessful(false);
		HttpPerformanceReportSample httpSample2 = new HttpPerformanceReportSample();
		httpSample2.setDuration(AVERAGE);
		httpSample2.setDate(date);
		httpSample2.setSuccessful(true);
		HttpPerformanceReportSample httpSample3 = new HttpPerformanceReportSample();
		httpSample3.setDuration(MIN);
		httpSample3.setDate(date);
		httpSample3.setSuccessful(false);
		uriReport.addHttpSample(httpSample1);
		uriReport.addHttpSample(httpSample2);
		uriReport.addHttpSample(httpSample3);
	}

	@Test
	public void testCountErrors() {
		assertEquals(2, uriReport.countErrors());
	}

	@Test
	public void testGetAverage() {
		assertEquals(AVERAGE, uriReport.getAverage());
	}

//	@Test
//	public void testGetMax() {
//		assertEquals(MAX, uriReport.getMax());
//	}

//	@Test
//	public void testGetMin() {
//		assertEquals(MIN, uriReport.getMin());
//	}

	@Test
	public void testIsFailed() {
		assertTrue(uriReport.isFailed());
	}

	/**
	 * Same dates, different duration. Shortest duration should be ordered first.
	 */
	@Test
	public void testCompareSameDateDifferentDuration() {
	  // setup fixture
    final List<Sample> samples = new ArrayList<Sample>();
	  samples.add( new Sample( new Date(1), 2) );
    samples.add( new Sample( new Date(1), 1) );
    
	  // execute system under test
    Collections.sort(samples);
	  
	  // verify result
    final Iterator<Sample> iter = samples.iterator();
    assertEquals(1, iter.next().duration );
    assertEquals(2, iter.next().duration );
	}
	
  /**
   * Different dates, same duration. Oldest date should be ordered first.
   */
  @Test
  public void testCompareDifferentDateSameDuration() {
    // setup fixture
    final List<Sample> samples = new ArrayList<Sample>();
    samples.add( new Sample( new Date(2), 1) );
    samples.add( new Sample( new Date(1), 1) );
    
    // execute system under test
    Collections.sort(samples);
    
    // verify result
    final Iterator<Sample> iter = samples.iterator();
    assertEquals(1, iter.next().date.getTime() );
    assertEquals(2, iter.next().date.getTime() );
  }
  
  /**
   * Different dates, different duration. Shortest duration should be ordered first.
   */
  @Test
  public void testCompareDifferentDateDifferentDuration() {
    // setup fixture
    final List<Sample> samples = new ArrayList<Sample>();
    samples.add( new Sample( new Date(1), 2) );
    samples.add( new Sample( new Date(2), 1) );
    
    // execute system under test
    Collections.sort(samples);
    
    // verify result
    final Iterator<Sample> iter = samples.iterator();
    assertEquals(1, iter.next().duration );
    assertEquals(2, iter.next().duration );
  }
  
  /**
   * Null dates. Ordering is unspecified, but should not cause exceptions.
   */
  @Test
  public void testCompareNullDateSameDuration() {
    // setup fixture
    final List<Sample> samples = new ArrayList<Sample>();
    samples.add( new Sample( null, 1) );
    samples.add( new Sample( null, 1) );
    
    try {
      // execute system under test
      Collections.sort(samples);
    } catch (NullPointerException e) {
      // verify result
      fail("A NullPointerException was thrown (which should not have happened).");
    }
  }
}
