package org.example.tinyhttp.http.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.example.tinyhttp.http.HttpExceptions;

public final class HttpParser {
    private HttpParser() {}
    
    public static String readLineCRLF(BufferedInputStream in, int maxBytes) throws IOException {
      int prev = -1, b, read = 0;
      ByteArrayOutputStream buf = new ByteArrayOutputStream();

      while((b = in.read()) != -1){
          if(++read > maxBytes) throw new HttpExceptions.LineTooLong("line too long");
          if((prev == '\r' && b == '\n') || b == '\n') {
              byte[] arr = buf.toByteArray();
              int length = (prev == '\r' && b == '\n') ? arr.length - 1 : arr.length;
              return new String(arr, 0, length, StandardCharsets.US_ASCII);
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

    public static byte[] readChunkedBody(BufferedInputStream in, int maxTotal) throws IOException, HttpExceptions.BadRequest {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int total = 0;

        while(true){
            // 1) Read chunk-size line: hex [; extensions] CRLF
            String line = readLineCRLF(in,8192);
            if (line == null) throw new IOException("EOF before chunk size");
            int semi = line.indexOf(";"); // Ignore extentions
            String hex = (semi > 0) ? line.substring(0, semi) : line;
            hex = hex.trim();
            if(hex.isEmpty()) throw new HttpExceptions.BadRequest("Empty chunk size");

            int size;
        
            if(!isValidHex(hex))
                throw new HttpExceptions.BadRequest("Invalid chunk size");
            // disallow negative & very large
            long val = Long.parseLong(hex, 16);
            if(val < 0 || val > Integer.MAX_VALUE) throw new HttpExceptions.BadRequest("Chunk size too large");
            size = (int) val;
            

            // 2) last chunk?
            if(size == 0){
                // read and ignore trailers until blank line
                while(true){
                    String t = readLineCRLF(in, 8192);
                    if(t == null) throw new IOException("EOF in trailers");
                    if(t.isEmpty()) break;
                }
                break; // done
            }
            
            // 3) Read excatly 'size' bytes then CRLF
            if(total + size > maxTotal){
                throw new HttpExceptions.BadRequest("Body too Large");
            }

            byte[] buf = in.readNBytes(size);
            if(buf.length != size) throw new IOException("EOF in chunked data");
            out.write(buf);
            total += size;

            // expect CRLF after each chunk
            int c1 = in.read(), c2 = in.read();
            if(c1 != '\r'|| c2 != '\n'){
                throw new  HttpExceptions.BadRequest("Missing CRLF after chunk");
            }

        }
        return out.toByteArray();

    }

    private static boolean isValidHex(String hex) {
        if (hex == null || hex.isEmpty()) return false;
        for (char c : hex.toCharArray()) {
            if (!((c >= '0' && c <= '9') || 
                  (c >= 'A' && c <= 'F') || 
                  (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }
}