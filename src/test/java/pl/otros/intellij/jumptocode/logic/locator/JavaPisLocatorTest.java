package pl.otros.intellij.jumptocode.logic.locator;

import org.junit.Assert;
import org.junit.Test;

public class JavaPisLocatorTest {


  private JavaPisLocator locator = new JavaPisLocator();


  @Test
  public void testUnwrap() throws Exception {
    Assert.assertEquals("some message", locator.unwrap("\"some message\""));
    Assert.assertEquals("some message\"", locator.unwrap("some message\""));
    Assert.assertEquals("\"some message", locator.unwrap("\"some message"));
  }

  @Test
  public void testWithoutArgsPlaceHolders() throws Exception {
    Assert.assertEquals("some ", locator.withoutArgsPlaceHolders("some %d message"));
    Assert.assertEquals(" some ", locator.withoutArgsPlaceHolders("%d some %d message"));
    Assert.assertEquals(" some ", locator.withoutArgsPlaceHolders(" %d some %d message"));
    Assert.assertEquals(" some ", locator.withoutArgsPlaceHolders("%4$2s some %d message"));
    Assert.assertEquals(" some ", locator.withoutArgsPlaceHolders("%+10.4f some %d message"));
    Assert.assertEquals("some msg abc ", locator.withoutArgsPlaceHolders("some msg abc %d message"));
    Assert.assertEquals("some msg abc ", locator.withoutArgsPlaceHolders("some msg abc {} message"));
    Assert.assertEquals(" msg abc message", locator.withoutArgsPlaceHolders("some {} msg abc message"));
    Assert.assertEquals(" some msg abc message", locator.withoutArgsPlaceHolders("{} some msg abc message"));
  }


  @Test
  public void testAbbreviateLinesShort() throws Exception {
    String[] a = new String[]{
        "1", "2", "3"
    };
    Assert.assertArrayEquals(a, locator.abbreviateLines(a, 5, 2));
  }

  @Test
  public void testAbbreviateLinesBorder() throws Exception {
    String[] a = new String[]{
        "1", "2", "3"
    };
    Assert.assertArrayEquals(a, locator.abbreviateLines(a, 3, 2));
  }

  @Test
  public void testAbbreviateLinesLong() throws Exception {
    String[] a = new String[]{
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"
    };
    String[] expected = new String[]{
        "1", "2", ".........", ".........", "8", "9", "10"
    };

    Assert.assertArrayEquals(expected, locator.abbreviateLines(a, 7, 2));
  }

  @Test
  public void testAbbreviateLinesLongCode() throws Exception {

    final String s = "    public final void request() {\n" +
        "        String user = null;\n" +
        "        final long timestamp = System.currentTimeMillis();\n" +
        "        try {\n" +
        "            final String requestUuid = UUID.randomUUID().toString().substring(0,8);\n" +
        "            MDC.put(MDC_REQ_ID, requestUuid);\n" +
        "            user = userIds.take();\n" +
        "            MDC.put(MDC_USER, user);\n" +
        "            LOGGER.error(String.format(\"Error on serving request\"));\n" +
        "            LOGGER.info(\"Request {} from customer\",new Throwable());\n" +
        "            performRequests();\n" +
        "            if (new Random().nextInt(100) == 0) {\n" +
        "                performPayments();\n" +
        "            }\n" +
        "            LOGGER.info(\"Sending response, processing have taken \" + (System.currentTimeMillis() - timestamp) + \"ms\");\n" +
        "        } catch (Exception e) {\n" +
        "            e.printStackTrace();\n" +
        "            LOGGER.error(String.format(\"Error on serving request\"), e);";
    String[] code18 = s.split("\n");

    String expectedS =
        "    public final void request() {\n" +
            "        String user = null;\n" +
            "        .........\n" +
            "        .........\n" +
            "        } catch (Exception e) {\n" +
            "            e.printStackTrace();\n" +
            "            LOGGER.error(String.format(\"Error on serving request\"), e);";

    final String[] actuals = locator.abbreviateLines(code18, 7, 2);
    Assert.assertArrayEquals(expectedS.split("\n"), actuals);
  }


}