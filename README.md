OtrosJumpToCode
===============
[![Codacy Badge](https://api.codacy.com/project/badge/grade/f226d72fad8d4a569e77eef237b9c01d)](https://www.codacy.com/app/krzysztof-otrebski/OtrosJumpToCode)

![Build status](https://api.travis-ci.org/otros-systems/OtrosJumpToCode.svg)


# Overview
[OtrosJumpToCode](https://plugins.jetbrains.com/plugin/7406-otrosjumptocode) is a plugin which can integrate OtrosLogViewer with Intellij. It allows OtrosLogViewer to:
 * Force Intellij to open selected class and highlight line where logger was called.
 * Get source with context where logger is called
 * Get logger patterns used in project
 
 # Details
 This plugin exposes HTTP endpoint which allows:
 * Jump to code in Intellij. To find location in source plugin can use:
   * file name and line number, for example `MyClass.java:144`
   * Class and log message. Plugin will try to figure out which Logger invokaction was used using log message.
 * Get context of code around specied location (file with line number or log message)
 * Extract loggers pattern layouts from log4 (xml, properties), log4j (xml, yaml, json, properties) and logback (xml)
  

Plugin is uploaded to [Intellij Plugins Repository](https://plugins.jetbrains.com/plugin/7406-otrosjumptocode)
 
  
 # Notes
This is a fork of JumpToCode Plugin in Intellij (http://plugins.jetbrains.com/plugin/1894?pr=)
