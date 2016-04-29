package hudson.plugins.performance.util;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.plugins.performance.report.PerformanceReportUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class IoUtil {

  public static final String ARTIFACT_ARCHIVE_DIRECTORY = "archive";

  public static void closeSilently(Closeable cls) {
    if (cls != null) {
      try {
        cls.close();
      } catch (IOException e) {
        // nop
      }
    }
  }

  public static File createXmlFile(AbstractBuild<?, ?> build, String name) throws IOException {
    String xmlDir = build.getRootDir().getAbsolutePath();
    xmlDir += "/" + ARTIFACT_ARCHIVE_DIRECTORY;

    String[] arr = name.split("/");
    if (!new File(xmlDir).exists()) {
      new File(xmlDir).mkdirs();
    }

    File xmlFile = new File(xmlDir + "/dashBoard_" + arr[arr.length - 1].split("\\.")[0] + ".xml");
    xmlFile.createNewFile();

    return xmlFile;
  }

  public static List<File> copyReportsToMaster(AbstractBuild<?, ?> build, PrintStream logger, List<FilePath> files, String parserDisplayName) throws IOException, InterruptedException {
    List<File> localReports = new ArrayList<File>();
    for (FilePath src : files) {
      final File localReport = PerformanceReportUtil.getPerformanceReport(build, parserDisplayName, src.getName());
      if (src.isDirectory()) {
        logger.println("Performance: File '" + src.getName() + "' is a directory, not a Performance Report");
        continue;
      }
      src.copyTo(new FilePath(localReport));
      localReports.add(localReport);
    }
    return localReports;
  }

}
