/*
 * Copyright 2014 otros.systems@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.otros.intellij.JumpToCode.logic;

import org.apache.log4j.Logger;
import pl.otros.intellij.JumpToCode.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * copied from spring-framework
 *
 * @author Juergen Hoeller
 * @since 06.10.2003
 */
public abstract class FileCopyUtils {

  public static final int BUFFER_SIZE = 4096;
  private static final Logger logger = Logger.getLogger(FileCopyUtils.class);

  //---------------------------------------------------------------------
  // Copy methods for java.io.InputStream / java.io.OutputStream
  //---------------------------------------------------------------------

  /**
   * Copy the contents of the given InputStream to the given OutputStream.
   * Closes both streams when done.
   *
   * @param in  the stream to copy from
   * @param out the stream to copy to
   * @return the number of bytes copied
   * @throws IOException in case of I/O errors
   */
  public static int copy(InputStream in, OutputStream out) throws IOException {
    try {
      int byteCount = 0;
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
        byteCount += bytesRead;
      }
      out.flush();
      return byteCount;
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }

}
