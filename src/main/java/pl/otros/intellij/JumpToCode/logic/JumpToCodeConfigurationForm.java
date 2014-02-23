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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;

/**
 */
public class JumpToCodeConfigurationForm {
  private JTextField hostnameField;
  private JPanel rootComponent;
  private JTextField portField;
  private JCheckBox enabledCheckBox;
  private JButton openOtrosLogViewerPageButton;
  private JButton openWikiPageButton;

  public JumpToCodeConfigurationForm() {
    openOtrosLogViewerPageButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          Desktop.getDesktop().browse(URI.create("http://code.google.com/p/otroslogviewer/"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });
    openWikiPageButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          Desktop.getDesktop().browse(URI.create("http://code.google.com/p/otroslogviewer/wiki/JumpToCode"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });
  }

  // Method returns the root component of the form
  public JComponent getRootComponent() {
    return rootComponent;
  }

  public void setData(Config data) {
    portField.setText(data.getPort());
    enabledCheckBox.setSelected(data.isEnabled());
    hostnameField.setText(data.getHostName());
  }

  public void getData(Config data) {
    data.setPort(portField.getText());
    data.setEnabled(enabledCheckBox.isSelected());
    data.setHostName(hostnameField.getText());
  }

  @SuppressWarnings({"RedundantIfStatement"})
  public boolean isModified(Config data) {
    if (portField.getText() != null ? !portField.getText().equals(data.getPort()) : data.getPort() != null)
      return true;
    if (enabledCheckBox.isSelected() != data.isEnabled())
      return true;
    if (hostnameField.getText() != null ? !hostnameField.getText().equals(data.getHostName()) : data.getHostName() != null)
      return true;
    return false;
  }
}
