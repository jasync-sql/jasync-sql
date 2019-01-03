package com.github.jasync.sql.db.util;

import io.netty.buffer.ByteBuf;

@SuppressWarnings({"StringConcatenationInsideStringBufferAppend", "Duplicates"})
public class BufferDumper {

    public static String dumpAsHex(ByteBuf buffer) {
        int length = buffer.readableBytes();
        byte[] byteBuffer = new byte[length];

        buffer.markReaderIndex();
        buffer.readBytes(byteBuffer);
        buffer.resetReaderIndex();

        StringBuilder outputBuf = new StringBuilder(length * 4);

        int p = 0;
        int rows = length / 8;

        for (int i = 0; (i < rows) && (p < length); i++) {
            int ptemp = p;

            outputBuf.append(i + ": ");

            for (int j = 0; j < 8; j++) {
                String hexVal = Integer.toHexString(byteBuffer[ptemp] & 0xff);

                if (hexVal.length() == 1) {
                    hexVal = "0" + hexVal;
                }

                outputBuf.append(hexVal + " ");
                ptemp++;
            }

            outputBuf.append("    ");

            for (int j = 0; j < 8; j++) {
                int b = 0xff & byteBuffer[p];

                if (b > 32 && b < 127) {
                    outputBuf.append((char) b + " ");
                } else {
                    outputBuf.append(". ");
                }

                p++;
            }

            outputBuf.append("\n");
        }

        outputBuf.append(rows + ": ");

        int n = 0;

        for (int i = p; i < length; i++) {
            String hexVal = Integer.toHexString(byteBuffer[i] & 0xff);

            if (hexVal.length() == 1) {
                hexVal = "0" + hexVal;
            }

            outputBuf.append(hexVal + " ");
            n++;
        }

        for (int i = n; i < 8; i++) {
            outputBuf.append("   ");
        }

        outputBuf.append("    ");

        for (int i = p; i < length; i++) {
            int b = 0xff & byteBuffer[i];

            if (b > 32 && b < 127) {
                outputBuf.append((char) b + " ");
            } else {
                outputBuf.append(". ");
            }
        }

        outputBuf.append("\n");
        outputBuf.append("Total " + byteBuffer.length + " bytes read\n");

        return outputBuf.toString();
    }

}
