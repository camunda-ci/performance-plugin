package hudson.plugins.performance.util;

public class AssertHelper {

  public static void checkNotNull(Object check) {
    if (check == null) {
      throw new NullPointerException("Null is not allowed for object.");
    }
  }

  public static void checkNotNull(String msg, Object check) {
    if (check == null) {
      throw new NullPointerException(msg);
    }
  }
}
