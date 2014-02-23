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

/**
 */
public class Config implements ServerConfig {
  
  private String hostName = "127.0.0.1";
  private int port = 5987;
  private boolean enabled = true;

  public String getHostName() {
    return hostName;
  }

  public int getPortNumber() {
    return port;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getPort() {
    return String.valueOf(port);
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public void setPort(String port) {
    try {
      this.port = Integer.parseInt(port);
    } catch (NumberFormatException e) {
      pl.otros.intellij.JumpToCode.logic.OtrosJumpToCodeApplicationComponent.LOGGER.info("user entered invalid port number: " + port);
      // TODO: show error dialog
    }
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
