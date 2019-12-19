package com.binchencoder.skylb.utils;

public final class PathUtil {

  /**
   * 得到文件名中的父路径部分。
   * 对两种路径分隔符都有效。
   * 不存在时返回""。
   * 如果文件名是以路径分隔符结尾的则不考虑该分隔符，例如"/path/"返回""。
   * @param fileName 文件名
   * @return 父路径，不存在或者已经是父目录时返回""
   */
  public static String getPathPart(String fileName) {
    int point = getPathLsatIndex(fileName);
    int length = fileName.length();
    if (point == -1) {
      return "";
    } else if (point == length - 1) {
      int secondPoint = getPathLsatIndex(fileName, point - 1);
      if (secondPoint == -1) {
        return "";
      } else {
        return fileName.substring(0, secondPoint);
      }
    } else {
      return fileName.substring(0, point);
    }
  }

  /**
   * 得到路径分隔符在文件路径中最后出现的位置。
   * 对于DOS或者UNIX风格的分隔符都可以。
   * @param fileName 文件路径
   * @return 路径分隔符在路径中最后出现的位置，没有出现时返回-1。
   */
  public static int getPathLsatIndex(String fileName) {
    int point = fileName.lastIndexOf('/');
    if (point == -1) {
      point = fileName.lastIndexOf("//");
    }
    return point;
  }

  /**
   * 得到路径分隔符在文件路径中指定位置前最后出现的位置。
   * 对于DOS或者UNIX风格的分隔符都可以。
   * @param fileName 文件路径
   * @param fromIndex 开始查找的位置
   * @return 路径分隔符在路径中指定位置前最后出现的位置，没有出现时返回-1。
   */
  public static int getPathLsatIndex(String fileName, int fromIndex) {
    int point = fileName.lastIndexOf('/', fromIndex);
    if (point == -1) {
      point = fileName.lastIndexOf("//", fromIndex);
    }
    return point;
  }
}
