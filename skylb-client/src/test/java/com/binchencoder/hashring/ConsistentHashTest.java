package com.binchencoder.hashring;

import com.binchencoder.skylb.balancer.consistenthash.Crc32Hash;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

/**
 * The test cases are adapted from @com_github_binchencoder_letsgo//hashring/hashring_test.go.
 *
 * @author fuyc@github.com/binchencoder
 */
public class ConsistentHashTest {

  ConsistentHashPrototype<String> conh;

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    conh = New();
  }

  ConsistentHashPrototype<String> New() {
    return new ConsistentHashPrototype<>(new Crc32Hash(), 20, Collections.EMPTY_LIST);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void TestGetEmpty() {
    Assert.assertNull(conh.get("asdfsadfsadf"));
  }

  class gtest {
    String in;
    String out;

    public gtest(String in, String out) {
      this.in = in;
      this.out = out;
    }
  }

  gtest[] gmtests = {
      new gtest("ggg", "abcdefg"),
      new gtest("hhh", "opqrstu"),
      new gtest("iiiii", "hijklmn")
  };

  @Test
  public void TestGetMultiple() {
    conh.add("abcdefg");
    conh.add("hijklmn");
    conh.add("opqrstu");
    for (gtest gtest : gmtests) {
      Assert.assertEquals(gtest.out, conh.get(gtest.in));
    }
  }

  gtest[] rtestsBefore = {
      new gtest("ggg", "abcdefg"),
      new gtest("hhh", "opqrstu"),
      new gtest("iiiii", "hijklmn")
  };

  gtest[] rtestsAfter = {
      new gtest("ggg", "abcdefg"),
      new gtest("hhh", "opqrstu"),
      new gtest("iiiii", "opqrstu")
  };

  @Test
  public void TestGetMultipleRemove() {
    conh.add("abcdefg");
    conh.add("hijklmn");
    conh.add("opqrstu");
    for (gtest gtest : rtestsBefore) {
      Assert.assertEquals(gtest.out, conh.get(gtest.in));
    }
    conh.remove("hijklmn");
    for (gtest gtest : rtestsAfter) {
      Assert.assertEquals(gtest.out, conh.get(gtest.in));
    }
  }

  @Test
  public void TestAddCollision() {
    final String s1 = "abear";
    final String s2 = "solidiform";

    ConsistentHashPrototype<String> x = New();
    x.add(s1);
    x.add(s2);
    String elt1 = x.get(s1);
    Assert.assertEquals(s2, elt1);

    ConsistentHashPrototype<String> y = New();
    y.add(s2);
    y.add(s1);
    Assert.assertEquals(elt1, y.get(s1));
  }
}
