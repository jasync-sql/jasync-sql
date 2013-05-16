package com.github.mauricio.async.db.mysql;

import org.jboss.netty.buffer.ChannelBuffer;

public class MySQLHelper {

    public static final String dumpAsHex(ChannelBuffer buffer) {
        int length = buffer.readableBytes();
        byte[] byteBuffer = new byte[length];

        buffer.markReaderIndex();
        buffer.readBytes(byteBuffer);
        buffer.resetReaderIndex();

        StringBuffer outputBuf = new StringBuffer(length * 4);

        int p = 0;
        int rows = length / 8;

        for (int i = 0; (i < rows) && (p < length); i++) {
            int ptemp = p;

            outputBuf.append(i + ": ");

            for (int j = 0; j < 8; j++) {
                String hexVal = Integer.toHexString(byteBuffer[ptemp] & 0xff);

                if (hexVal.length() == 1) {
                    hexVal = "0" + hexVal; //$NON-NLS-1$
                }

                outputBuf.append(hexVal + " "); //$NON-NLS-1$
                ptemp++;
            }

            outputBuf.append("    "); //$NON-NLS-1$

            for (int j = 0; j < 8; j++) {
                int b = 0xff & byteBuffer[p];

                if (b > 32 && b < 127) {
                    outputBuf.append((char) b + " "); //$NON-NLS-1$
                } else {
                    outputBuf.append(". "); //$NON-NLS-1$
                }

                p++;
            }

            outputBuf.append("\n"); //$NON-NLS-1$
        }

        outputBuf.append(rows + ": ");

        int n = 0;

        for (int i = p; i < length; i++) {
            String hexVal = Integer.toHexString(byteBuffer[i] & 0xff);

            if (hexVal.length() == 1) {
                hexVal = "0" + hexVal; //$NON-NLS-1$
            }

            outputBuf.append(hexVal + " "); //$NON-NLS-1$
            n++;
        }

        for (int i = n; i < 8; i++) {
            outputBuf.append("   "); //$NON-NLS-1$
        }

        outputBuf.append("    "); //$NON-NLS-1$

        for (int i = p; i < length; i++) {
            int b = 0xff & byteBuffer[i];

            if (b > 32 && b < 127) {
                outputBuf.append((char) b + " "); //$NON-NLS-1$
            } else {
                outputBuf.append(". "); //$NON-NLS-1$
            }
        }

        outputBuf.append("\n"); //$NON-NLS-1$
        outputBuf.append("Total " + byteBuffer.length + " bytes read\n");

        return outputBuf.toString();
    }

}