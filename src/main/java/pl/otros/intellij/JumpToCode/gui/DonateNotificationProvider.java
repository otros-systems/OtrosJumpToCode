package pl.otros.intellij.JumpToCode.gui;


import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.otros.intellij.JumpToCode.Properties;

public class DonateNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
  public static final String DONATION_URL =
      "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&" +
          "business=GJUQP3X5FMUQU&lc=US&item_name=OtrosLogViewer-Intellij%20%2d%20donate&" +
          "currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted";

  private static final Key<EditorNotificationPanel> KEY = Key.create("Donate me");

  private final EditorNotifications notifications;

  //    public DonateNotificationProvider(Project project, @NotNull EditorNotifications notifications) {
  public DonateNotificationProvider(@NotNull EditorNotifications notifications) {
    this.notifications = notifications;
  }

  @Override
  @NotNull
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
    if (!Properties.displayDonations()) {
      return null;
    }
    return createPanel();
  }

  private EditorNotificationPanel createPanel() {
    final EditorNotificationPanel panel = new EditorNotificationPanel();
    final String message = String.format("You have performed %d jumps from OtrosLogViewer", Properties.getJumpsCount());
    panel.setText(message);
    panel.createActionLabel("Donate me", new Runnable() {
      @Override
      public void run() {
        BrowserUtil.browse(DONATION_URL);
        Properties.setIgnoreDonationForNextNJumps();
        notifications.updateAllNotifications();
      }
    });
    panel.createActionLabel("Cancel", new Runnable() {
      @Override
      public void run() {
        Properties.setIgnoreDonationForNextNJumps();
        notifications.updateAllNotifications();
      }
    });

    try { // ignore if older SDK does not support panel icon
      panel.icon(AllIcons.Icons.Ide.NextStep);
    } catch (NoSuchMethodError ignored) {
    }

    return panel;
  }
}
