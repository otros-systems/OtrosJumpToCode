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

import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class FileUtilsTest {
  @Test
  public void testReadFileSelectedLines() throws Exception {
    //given
    StringBuilder sb = new StringBuilder();
    for (int i=1; i<120;i++){
      sb.append("This is line ").append(i).append("\n");
    }

    //when
    StringBuilder outputSb = new StringBuilder();
    FileUtils.readFileSelectedLines(102,new ByteArrayInputStream(sb.toString().getBytes()), outputSb);

    //then
    String expected="100: This is line 100\n"+
    "101: This is line 101\n"+
    "102: This is line 102\n"+
    "103: This is line 103\n"+
    "104: This is line 104\n";
    Assert.assertEquals(expected, outputSb.toString());


  }
}
