/*-------------------------------------------------------------------------
*
* Copyright (c) 2003-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/util/MD5Digest.java,v 1.13 2011/08/02 13:50:29 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package com.github.mauricio.postgresql.util;

import org.jboss.netty.util.CharsetUtil;

import java.security.MessageDigest;

/**
 * MD5-based utility function to obfuscate passwords before network
 * transmission.
 *
 * @author Jeremy Wohl
 */

public class PostgreSQLMD5Digest
{
    private static final byte[] lookup = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

    private PostgreSQLMD5Digest()
    {
    }

    /*
     * Encodes user/password/salt information in the following way:
     *  MD5(MD5(password + user) + salt)
     *
     * @param user  The connecting user.
     * @param password The connecting user's password.
     * @param salt  A four-salt sent by the server.
     *
     * @return A 35-byte array, comprising the string "md5" and an MD5 digest.
     */
    public static byte[] encode(String userStr, String passwordStr, byte[] salt)
    {
        byte[] user = userStr.getBytes(CharsetUtil.UTF_8);
        byte[] password = passwordStr.getBytes(CharsetUtil.UTF_8);

        MessageDigest md;
        byte[] temp_digest, pass_digest;
        byte[] hex_digest = new byte[35];

        try
        {
            md = MessageDigest.getInstance("MD5");

            md.update(password);
            md.update(user);
            temp_digest = md.digest();

            bytesToHex(temp_digest, hex_digest, 0);
            md.update(hex_digest, 0, 32);
            md.update(salt);
            pass_digest = md.digest();

            bytesToHex(pass_digest, hex_digest, 3);
            hex_digest[0] = 'm';
            hex_digest[1] = 'd';
            hex_digest[2] = '5';
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }

        return hex_digest;
    }


    /*
     * Turn 16-byte stream into a human-readable 32-byte hex string
     */
    private static void bytesToHex(byte[] bytes, byte[] hex, int offset)
    {

        int i, c, j, pos = offset;

        for (i = 0; i < 16; i++)
        {
            c = bytes[i] & 0xFF;
            j = c >> 4;
            hex[pos++] = lookup[j];
            j = (c & 0xF);
            hex[pos++] = lookup[j];
        }
    }
}