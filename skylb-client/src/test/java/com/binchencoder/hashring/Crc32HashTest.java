package com.binchencoder.hashring;

import com.binchencoder.skylb.balancer.consistenthash.Crc32Hash;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The test cases are adapted from @com_github_binchencoder_letsgo//hashring/hashring_test.go.
 *
 * @author binchencoder@github.com/binchencoder
 */
public class Crc32HashTest {

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
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for {@link com.binchencoder.hashring.Crc32Hash#hash(java.lang.String)}.
   */
  @Test
  public void testHash() {
    System.out.println(new Crc32Hash().hash("abear"));
    System.out.println(new Crc32Hash().hash("solidiform"));
  }
}
