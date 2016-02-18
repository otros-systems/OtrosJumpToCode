package pl.otros.intellij.JumpToCode.gui;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class SwingUtils {

  private final static Logger LOGGER = Logger.getLogger(SwingUtils.class);

  public static void invokeSwing(Runnable runnable, boolean wait) {
    try {
      if (wait) {
        SwingUtilities.invokeAndWait(runnable);
      } else {
        SwingUtilities.invokeLater(runnable);
      }
    } catch (InterruptedException e) {
      LOGGER.error("Interrupted", e);
    } catch (InvocationTargetException e) {
      LOGGER.error("InvocationTargetException", e);
    }
  }
}
