package com.github.mauricio.async.db.postgresql.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class PasswordHelper {

  private static final byte[] Lookup = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f'};

  private static void bytesToHex(byte[] bytes, byte[] hex, int offset) {

    int pos = offset;
    int i = 0;

    while ( i < 16 ) {
      int c = bytes[i] & 0xff;
      int j = c >> 4;
      hex[pos] = Lookup[j];
      pos += 1;
      j = (c & 0xf);

      hex[pos] = Lookup[j];
      pos += 1;

      i += 1;
    }

  }

  public static byte[] encode( String userText, String passwordText,byte[] salt,Charset charset) throws NoSuchAlgorithmException {
    byte[] user = userText.getBytes(charset);
    byte[] password = passwordText.getBytes(charset);

    MessageDigest md = MessageDigest.getInstance("MD5");

    md.update(password);
    md.update(user);

    byte[] tempDigest = md.digest();

    byte[] hexDigest = new byte[35];

    bytesToHex(tempDigest, hexDigest, 0);
    md.update(hexDigest, 0, 32);
    md.update(salt);

    byte[] passDigest = md.digest();

    bytesToHex(passDigest, hexDigest, 3);

    hexDigest[0] = 'm';
    hexDigest[1] = 'd';
    hexDigest[2] = '5';

    return hexDigest;
  }

}
