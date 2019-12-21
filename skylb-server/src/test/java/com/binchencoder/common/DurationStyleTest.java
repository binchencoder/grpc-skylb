package com.binchencoder.common;

import org.junit.Assert;
import org.junit.Test;

public class DurationStyleTest {

  @Test
  public void detectAndParse() {
    Assert.assertEquals(DurationStyle.detectAndParse("2s").toMillis(), 2000l);

    Assert.assertEquals(DurationStyle.detectAndParse("2ms").toMillis(), 2l);

    Assert.assertEquals(DurationStyle.detectAndParse("2h").toMinutes(), 120l);

    Assert.assertEquals(DurationStyle.detectAndParse("2s").toMinutes(), 0);
  }

  @Test/*(expected = IllegalArgumentException.class)*/
  public void detectAndParseException() {
    Assert.assertEquals(DurationStyle.detectAndParse("2 SS").toMillis(), 2000l);
  }

  @Test
  public void detect() {
  }
}