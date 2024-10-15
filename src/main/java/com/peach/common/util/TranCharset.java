package com.peach.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //判断字符串编码方式
 * @CreateTime 2024/10/14 15:51
 */
@Slf4j
public class TranCharset {

  private static final String[] ENCODINGS = new String[] { "GB2312", "ISO-8859-1", "UTF-8", "GBK" };

  /**
   * 获取字符集编码
   * @param str
   * @return
   */
  public static String getEncoding(String str) {
    if (str == null) {
      return StringUtil.EMPTY;
    }
    for (String encoding : ENCODINGS) {
      try {
        // 先将字符串编码为字节数组，再用相同编码解码回来
        if (str.equals(new String(str.getBytes(encoding), encoding))) {
          return encoding; // 如果编码匹配，返回编码名
        }
      } catch (Exception e) {
        log.error("" +e.getMessage(),e);
      }
    }

    return StringUtil.EMPTY; // 返回空字符串表示未找到匹配的编码
  }
}