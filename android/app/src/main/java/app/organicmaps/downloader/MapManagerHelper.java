package app.organicmaps.downloader;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import app.organicmaps.R;
import app.organicmaps.sdk.downloader.CountryItem;
import app.organicmaps.sdk.downloader.ExpandRetryConfirmationListener;
import app.organicmaps.sdk.downloader.MapManager;
import app.organicmaps.sdk.downloader.UpdateInfo;
import app.organicmaps.sdk.util.ConnectionState;
import app.organicmaps.util.StorageSpaceMonitor;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.lang.ref.WeakReference;

public class MapManagerHelper
{
  private static WeakReference<AlertDialog> sCurrentErrorDialog;

  public static void showError(final Context context, final MapManager.StorageCallbackData errorData,
                               @Nullable final Consumer<Boolean> dialogClickListener)
  {
    if (!MapManager.nativeIsAutoretryFailed())
      return;

    showErrorDialog(context, errorData, dialogClickListener);
  }

  public static void showErrorDialog(final Context context, final MapManager.StorageCallbackData errorData,
                                     @Nullable final Consumer<Boolean> dialogClickListener)
  {
    if (sCurrentErrorDialog != null)
    {
      AlertDialog dlg = sCurrentErrorDialog.get();
      if (dlg != null && dlg.isShowing())
        return;
    }

    final AlertDialog dlg = new MaterialAlertDialogBuilder(context, R.style.MwmTheme_AlertDialog)
                                .setTitle(R.string.country_status_download_failed)
                                .setMessage(ErrorCodeHelper.getErrorCodeStrRes(errorData.errorCode))
                                .setNegativeButton(R.string.cancel,
                                                   (dialog, which) -> {
                                                     sCurrentErrorDialog = null;
                                                     if (dialogClickListener != null)
                                                       dialogClickListener.accept(false);
                                                   })
                                .setPositiveButton(R.string.downloader_retry,
                                                   (dialog, which) -> {
                                                     ExpandRetryConfirmationListener listener =
                                                         new ExpandRetryConfirmationListener(dialogClickListener);
                                                     warn3gAndRetry(context, errorData.countryId, listener);
                                                   })
                                .create();
    dlg.setCanceledOnTouchOutside(false);
    dlg.show();
    sCurrentErrorDialog = new WeakReference<>(dlg);
  }

  private static void notifyNoSpaceInternal(Context context)
  {
    new MaterialAlertDialogBuilder(context, R.style.MwmTheme_AlertDialog)
        .setTitle(R.string.downloader_no_space_title)
        .setMessage(R.string.downloader_no_space_message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  /**
   * @return true if there is no space to update the given {@code root}, so the alert dialog will be shown.
   */
  private static boolean notifyNoSpaceToUpdate(Context context, String root)
  {
    if (MapManager.nativeHasSpaceToUpdate(root))
      return false;

    notifyNoSpaceInternal(context);
    return true;
  }

  /**
   * @return true if there is no space to download the given {@code root}, so the alert dialog will be shown.
   */
  private static boolean notifyNoSpace(Context context, String root)
  {
    if (MapManager.nativeHasSpaceToDownloadCountry(root))
      return false;

    notifyNoSpaceInternal(context);
    return true;
  }

  /**
   * @return true if there is no space to download {@code size} bytes, so the alert dialog will be shown.
   */
  private static boolean notifyNoSpace(Context context, long size)
  {
    if (MapManager.nativeHasSpaceToDownloadAmount(size))
      return false;

    notifyNoSpaceInternal(context);
    return true;
  }

  private static boolean warnOn3gInternal(Context context, @NonNull final Runnable onAcceptListener)
  {
    if (MapManager.nativeIsDownloadOn3gEnabled() || !ConnectionState.INSTANCE.isMobileConnected())
    {
      onAcceptListener.run();
      return false;
    }

    new MaterialAlertDialogBuilder(context, R.style.MwmTheme_AlertDialog)
        .setTitle(R.string.download_over_mobile_header)
        .setMessage(R.string.download_over_mobile_message)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.ok,
                           (dlg, which) -> {
                             MapManager.nativeEnableDownloadOn3g();
                             onAcceptListener.run();
                           })
        .show();

    return true;
  }

  // Fork F2: soft warning when a download would leave less free space than the low storage threshold.
  // Runs after the hard "Not enough space" check and before the mobile data warning, which it then chains to.
  private static boolean warnLowSpaceAndOn3g(Context context, long downloadBytes, @NonNull Runnable onAcceptListener)
  {
    final long freeBytes = StorageSpaceMonitor.getFreeBytes();
    if (freeBytes == StorageSpaceMonitor.UNKNOWN)
      return warnOn3gInternal(context, onAcceptListener);
    final long freeAfterBytes = Math.max(0, freeBytes - downloadBytes);
    if (!StorageSpaceMonitor.isBelowThreshold(context, freeAfterBytes))
      return warnOn3gInternal(context, onAcceptListener);

    final String freeAfter = StorageSpaceMonitor.formatSize(context, freeAfterBytes);
    final String threshold = StorageSpaceMonitor.formatSize(context, StorageSpaceMonitor.getThresholdBytes(context));
    new MaterialAlertDialogBuilder(context, R.style.MwmTheme_AlertDialog)
        .setTitle(R.string.low_storage_title)
        .setMessage(context.getString(R.string.download_low_storage_message, freeAfter, threshold))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.download, (dlg, which) -> warnOn3gInternal(context, onAcceptListener))
        .show();
    return true;
  }

  private static long getDownloadSize(@Nullable String countryId)
  {
    if (TextUtils.isEmpty(countryId))
      return 0;
    final CountryItem item = CountryItem.fill(countryId);
    // totalSize is the whole node, size is what is already on the device.
    return Math.max(0, item.totalSize - item.size);
  }

  private static long getUpdateSize(@Nullable String countryId)
  {
    if (TextUtils.isEmpty(countryId))
      return 0;
    final UpdateInfo info = MapManager.nativeGetUpdateInfo(countryId);
    return info == null ? 0 : info.totalSize;
  }

  public static boolean warnOn3gUpdate(Context context, @Nullable String countryId,
                                       @NonNull final Runnable onAcceptListener)
  {
    // noinspection SimplifiableIfStatement
    if (TextUtils.isEmpty(countryId) || !notifyNoSpaceToUpdate(context, countryId))
      return warnLowSpaceAndOn3g(context, getUpdateSize(countryId), onAcceptListener);

    return true;
  }

  public static boolean warnOn3g(Context context, @Nullable String countryId, @NonNull final Runnable onAcceptListener)
  {
    // noinspection SimplifiableIfStatement
    if (TextUtils.isEmpty(countryId) || !notifyNoSpace(context, countryId))
      return warnLowSpaceAndOn3g(context, getDownloadSize(countryId), onAcceptListener);

    return true;
  }

  public static boolean warnOn3g(Context context, long size, @NonNull Runnable onAcceptListener)
  {
    return !notifyNoSpace(context, size) && warnLowSpaceAndOn3g(context, size, onAcceptListener);
  }

  public static boolean warn3gAndDownload(Context context, final String countryId,
                                          @Nullable final Runnable onAcceptListener)
  {
    return warnOn3g(context, countryId, () -> {
      if (onAcceptListener != null)
        onAcceptListener.run();
      startDownload(context, countryId);
    });
  }

  public static boolean warn3gAndRetry(Context context, final String countryId,
                                       @Nullable final Runnable onAcceptListener)
  {
    return warnOn3g(context, countryId, () -> {
      if (onAcceptListener != null)
        onAcceptListener.run();
      retryDownload(context, countryId);
    });
  }

  /**
   * Enqueues failed items under given {@code root} node in downloader.
   * If the service is already running, enqueues directly; otherwise starts the service first.
   */
  public static void retryDownload(Context context, @NonNull String countryId)
  {
    if (MapManager.nativeIsDownloading())
      MapManager.retryDownload(countryId);
    else
      DownloaderService.startRetryDownload(context, countryId);
  }

  /**
   * Enqueues given {@code root} node with its children in downloader.
   * If the service is already running, enqueues directly; otherwise starts the service first.
   */
  public static void startUpdate(Context context, @NonNull String root)
  {
    if (MapManager.nativeIsDownloading())
      MapManager.startUpdate(root);
    else
      DownloaderService.startUpdate(context, root);
  }

  /**
   * Enqueues the given list of nodes and its children in downloader.
   * If the service is already running, enqueues directly; otherwise starts the service first.
   */
  public static void startDownload(Context context, String... countries)
  {
    if (MapManager.nativeIsDownloading())
    {
      for (var countryId : countries)
        MapManager.startDownload(countryId);
    }
    else
      DownloaderService.startDownload(context, countries);
  }

  /**
   * Enqueues given {@code root} node and its children in downloader.
   * If the service is already running, enqueues directly; otherwise starts the service first.
   */
  public static void startDownload(Context context, @NonNull String countryId)
  {
    if (MapManager.nativeIsDownloading())
      MapManager.startDownload(countryId);
    else
      DownloaderService.startDownload(context, countryId);
  }
}
