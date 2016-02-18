package pl.otros.intellij.jumptocode.logic.locator;

import pl.otros.intellij.jumptocode.model.JumpLocation;

import java.util.List;

public interface Locator {
  List<? extends JumpLocation> findLocation(LocationInfo locationInfo);

  List<String> getContent(LocationInfo locationInfo);
}
