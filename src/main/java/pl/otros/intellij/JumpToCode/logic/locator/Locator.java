package pl.otros.intellij.JumpToCode.logic.locator;

import pl.otros.intellij.JumpToCode.model.JumpLocation;

import java.util.List;

public interface Locator {
  List<? extends JumpLocation> findLocation(LocationInfo locationInfo);

  List<String> getContent(LocationInfo locationInfo);
}
