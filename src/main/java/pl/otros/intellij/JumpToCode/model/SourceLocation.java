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

package pl.otros.intellij.JumpToCode.model;

import com.intellij.openapi.util.text.StringUtil;

/**
 */
public class SourceLocation implements JumpLocation {

  private String packageName;

  private String fileName;

  private int lineNumber;


  public SourceLocation(String packageName, String fileName, int lineNumber) {
    this.packageName = packageName;
    this.fileName = fileName;
    this.lineNumber = lineNumber;
  }

  /**
   * construct a SourceLocation from a fully qualified class name (not yet used)
   *
   * @param fullyQualifiedClassName fully qualified class name
   */
  public SourceLocation(String fullyQualifiedClassName) {
    this.packageName = StringUtil.getPackageName(fullyQualifiedClassName);
    // determine filename from the className
    String shortName = StringUtil.getShortName(fullyQualifiedClassName);
    // handle inner classes
    // (won't work when class has one or more dollar signs in his name)
    int index = shortName.indexOf("$");
    if (index != -1) {
      this.fileName = shortName.substring(0, index) + ".java";
    } else {
      this.fileName = fileName + ".java";
    }
    this.lineNumber = -1;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getPackageName() {
    return packageName;
  }

  @Override
  public String toString() {
    return packageName + "(" + fileName + ":" + lineNumber + "}";
  }

  public String getFileName() {
    return fileName;
  }

}
