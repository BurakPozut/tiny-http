package org.example.tinyhttp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class HttpParser {
    private HttpParser() {}
    
    public static String readLineCRLF(BufferedInputStream in, int maxBytes) throws IOException {
      int prev = -1, b, read = 0;
      ByteArrayOutputStream buf = new ByteArrayOutputStream();

      while((b = in.read()) != -1){
          if(++read > maxBytes) throw new HttpExceptions.LineTooLong("line too long");
          if(prev == '\r' && b == '\n') {
              byte[] arr = buf.toByteArray();
              return new String(arr, 0, arr.length - 1, StandardCharsets.US_ASCII);
          }

          buf.write(b);
          prev = b;
      }
      return null;
    }
    
    public static String readLineCRLF(BufferedInputStream in) throws IOException {
        return readLineCRLF(in, Integer.MAX_VALUE);
    }

    public static byte[] readFixedBytes(BufferedInputStream in, long length) throws
    IOException, HttpExceptions.BadRequest{
        if(length > Integer.MAX_VALUE)
            throw new HttpExceptions.BadRequest("Body too large");

        int toRead = (int) length;
        byte[] buff = new byte[toRead];
        int totalRead = 0;

        while(totalRead < toRead){
            int bytesRead = in.read(buff,totalRead, toRead - totalRead);
            if (bytesRead == -1) {
                throw new HttpExceptions.BadRequest("Incomplete Request Body");
            }
            totalRead += bytesRead;
        }
        return buff;
    }
}