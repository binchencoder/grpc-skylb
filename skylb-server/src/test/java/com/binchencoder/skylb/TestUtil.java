package com.binchencoder.skylb;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.binchencoder.skylb.utils.PathUtil;
import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;
import io.etcd.jetcd.ByteSequence;
import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.Test;

public class TestUtil {

  public static ByteSequence bytesOf(final String string) {
    return ByteSequence.from(string, UTF_8);
  }

  public static ByteString byteStringOf(final String string) {
    return ByteString.copyFrom(string.getBytes(UTF_8));
  }

  public static String randomString() {
    return java.util.UUID.randomUUID().toString();
  }

  public static ByteSequence randomByteSequence() {
    return ByteSequence.from(randomString(), Charsets.UTF_8);
  }

  public static int findNextAvailablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  public static void closeQuietly(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (final IOException ioe) {
      // ignore
    }
  }


  @Test
  public void testGetPathPart() {
    String fileName = "//registry/services..//endpoints//111.txt";
    System.out.println(PathUtil.getPathPart(fileName));

    System.out.println(PathUtil.getPathPart(
        "/registry/services/endpoints/default/custom-ease-gateway-test/127.0.0.1_9090"));
  }
}
