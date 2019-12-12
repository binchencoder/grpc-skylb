package com.beust.jcommander;

import static org.junit.Assert.assertEquals;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(separators = "=")
class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  static PrintWriter out;

  @Parameter(names = {"--length", "--l"}, description = "Length", required = true)
  int length = 123;

  @Parameter(names = {"--pattern", "--p"}, description = "Pattern")
  int pattern = 111;

  @Parameter(names = "--help", help = true)
  private boolean help;

  public static void main(String... argv) {
//    out = new PrintWriter(new StringWriter());

    Main args = new Main();
    JCommander commander = JCommander.newBuilder().addObject(args).build();
    commander.parse(argv);
    commander.usage();

    args.run();
  }

  public void run() {
    LOGGER.info("--length={} --pattern={}", length, pattern);
    out.printf("%d %d", length, pattern);
  }
}

public class SimpleExample {

  StringWriter out;

  @Before
  public void setupMain() {
    out = new StringWriter();
    Main.out = new PrintWriter(out);
  }

  @Test
  public void testLongArgs() {
    Main.main("--length=512", "--pattern=2");
    assertEquals("512 2", out.toString());
  }

  @Test
  public void testShortArgs() {
    Main.main("--l=256", "--p=171");
    assertEquals("256 171", out.toString());
  }

}
