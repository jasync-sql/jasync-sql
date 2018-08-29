package com.github.jasync.sql.db.util;

/**
 * The code from this class was copied from the Hex class at commons-codec
 */

class HexCodec {

  private static final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  private static int toDigit(char ch, int index) {
    int digit = Character.digit(ch, 16);

    if (digit == -1) {
      throw new IllegalArgumentException("Illegal hexadecimal character " + ch + " at index " + index);
    }

    return digit;
  }

  /**
   * Turns a HEX based char sequence into a Byte array
   *
   * @param value
   * @param start
   * @return
   */

  //start 0
  public static byte[] decode(CharSequence value, int start) {

    int length = value.length() - start;
    int end = value.length();

    if ((length & 0x01) != 0) {
      throw new IllegalArgumentException("Odd number of characters. A hex encoded byte array has to be even.");
    }

    byte[] out = new byte[length >> 1];

    int i = 0;
    int j = start;

    while (j < end) {
      int f = toDigit(value.charAt(j), j) << 4;
      j += 1;
      f = f | toDigit(value.charAt(j), j);
      j += 1;
      out[i] = (byte) (f & 0xff);
      i += 1;
    }

    return out;
  }

  /**
   * Encodes a byte array into a String encoded with Hex values.
   *
   * @param bytes
   * @param prefix
   * @return
   */

  public static String encode(byte[] bytes, char[] prefix) {
    int length = (bytes.length * 2) + prefix.length;
    char[] chars = new char[length];

    if (prefix.length != 0) {
      int x = 0;
      while (x < prefix.length) {
        chars[x] = prefix[x];
        x += 1;
      }
    }

    int dataLength = bytes.length;
    int j = prefix.length;
    int i = 0;

    while (i < dataLength) {
      chars[j] = digits[(0xF0 & bytes[i]) >>> 4];
      j += 1;
      chars[j] = digits[0x0F & bytes[i]];
      j += 1;
      i += 1;
    }

    return new String(chars);
  }

}
