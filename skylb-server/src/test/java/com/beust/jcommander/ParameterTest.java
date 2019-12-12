package com.beust.jcommander;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ParameterTest {

  @Parameters(separators = "=")
  private static class ManualOrder1 {

    @Parameter(order = 1, names = "--arg_b")
    public boolean isB;

    @Parameter(order = 0, names = "--arg_a")
    public boolean isA;

    @Parameter(order = 2, names = "--arg_c")
    public boolean isC;
  }

  @Test
  public void testOrder1() {
    testOrder(new ManualOrder1(), "--arg_a","--arg_b","--arg_c");
  }

  public void testOrder(Object cmd, String ... expected) {
    JCommander commander = new JCommander(cmd);

    StringBuilder out = new StringBuilder();
    commander.getUsageFormatter().usage(out);
    String output = out.toString();
    List<String> order = new ArrayList<>();
    for (String line : output.split("[\\n\\r]+")) {
      String trimmed = line.trim();
      if (!trimmed.contains(":")) {
        order.add(trimmed);
      }
    }
    Assert.assertEquals(order, Arrays.asList(expected));
  }

}
