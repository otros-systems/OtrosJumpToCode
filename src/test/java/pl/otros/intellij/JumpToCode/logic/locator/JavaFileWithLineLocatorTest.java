package pl.otros.intellij.JumpToCode.logic.locator;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class JavaFileWithLineLocatorTest {

  @Test
  public void testReadFileSelectedLines() throws Exception {
    //given
    StringBuilder sb = new StringBuilder();
    for (int i=1; i<120;i++){
      sb.append("This is line ").append(i).append("\n");
    }

    //when
    StringBuilder outputSb = new StringBuilder();
    new JavaFileWithLineLocator().readFileSelectedLines(102,new ByteArrayInputStream(sb.toString().getBytes()), outputSb);

    //then
    String expected="100: This is line 100\n"+
        "101: This is line 101\n"+
        "102: This is line 102\n"+
        "103: This is line 103\n"+
        "104: This is line 104\n";
    Assert.assertEquals(expected, outputSb.toString());
  }
}