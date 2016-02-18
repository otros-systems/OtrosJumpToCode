/*
 * The MIT License (MIT)
 *
 * Copyright (c) today.year hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pl.otros.intellij.JumpToCode;

import com.intellij.ide.util.PropertiesComponent;

import java.util.Random;

public class Properties {

  private static final String PROP_IGNORE_DONATION = "ignore_donation";
  private static final String PROP_JUMP_COUNT = "JUMP_COUNT";
  public static final int JUMP_THRESHOLD = 1024;

  private Properties() {
  }

  public static void setIgnoreDonationForNextNJumps() {
    final int increase = new Random().nextInt(8) + 1;
    final int newThreshold = getJumpsCount() - getJumpsCount() % JUMP_THRESHOLD + JUMP_THRESHOLD * increase;
    PropertiesComponent.getInstance().setValue(PROP_IGNORE_DONATION, Integer.toString(
        newThreshold));
  }


  public static boolean displayDonations() {
    final int indoreThreshold = PropertiesComponent.getInstance().getInt(PROP_IGNORE_DONATION, JUMP_THRESHOLD);
    final int jumpsCount = getJumpsCount();
    return jumpsCount >= indoreThreshold && jumpsCount >= JUMP_THRESHOLD;
  }

  public static int getJumpsCount() {
    return PropertiesComponent.getInstance().getInt(PROP_JUMP_COUNT, 0);
  }

  public static void increaseJumpsCount() {
    final int i = getJumpsCount() + 1;
    PropertiesComponent.getInstance().setValue(PROP_JUMP_COUNT, Integer.toString(i));
  }

}
