package app.organicmaps.util;

import android.content.Context;
import android.text.format.Formatter;
import androidx.annotation.NonNull;
import app.organicmaps.MwmApplication;
import app.organicmaps.sdk.Framework;
import app.organicmaps.sdk.util.log.Logger;
import java.io.File;

/**
 * Free space checks for the maps directory, used to warn the user before the device runs out of storage.
 * The threshold is a fixed amount (not a percentage) chosen in the settings; 0 disables the warnings.
 */
public final class StorageSpaceMonitor
{
  private static final String TAG = StorageSpaceMonitor.class.getSimpleName();

  public static final String PREF_THRESHOLD_MB = "LowStorageThresholdMb";
  public static final int DEFAULT_THRESHOLD_MB = 500;
  // Decimal megabytes, to match what Formatter.formatShortFileSize() shows to the user.
  private static final long BYTES_IN_MB = 1000L * 1000L;
  public static final long UNKNOWN = -1;

  private static boolean sStartupWarningShown = false;

  private StorageSpaceMonitor() {}

  /**
   * @return usable bytes in the maps directory, or {@link #UNKNOWN} if the directory is not available
   * (e.g. an unmounted SD card, for which getUsableSpace() would report 0 and cause a false alarm).
   */
  public static long getFreeBytes()
  {
    final File dir = new File(Framework.nativeGetWritableDir());
    if (!dir.isDirectory())
    {
      Logger.w(TAG, "Writable dir is not available: " + dir.getPath());
      return UNKNOWN;
    }
    final long free = dir.getUsableSpace();
    Logger.d(TAG, "Free space in " + dir.getPath() + ": " + free + " bytes");
    return free;
  }

  public static int getThresholdMb(@NonNull Context context)
  {
    return MwmApplication.prefs(context).getInt(PREF_THRESHOLD_MB, DEFAULT_THRESHOLD_MB);
  }

  public static void setThresholdMb(@NonNull Context context, int thresholdMb)
  {
    MwmApplication.prefs(context).edit().putInt(PREF_THRESHOLD_MB, thresholdMb).apply();
  }

  public static long getThresholdBytes(@NonNull Context context)
  {
    return getThresholdMb(context) * BYTES_IN_MB;
  }

  /** @return true if {@code freeBytes} is known and below the threshold; always false when warnings are off. */
  public static boolean isBelowThreshold(@NonNull Context context, long freeBytes)
  {
    final long threshold = getThresholdBytes(context);
    return threshold > 0 && freeBytes != UNKNOWN && freeBytes < threshold;
  }

  /** @return true at most once per process, and only when the free space is below the threshold. */
  public static boolean shouldShowStartupWarning(@NonNull Context context)
  {
    if (sStartupWarningShown || !isBelowThreshold(context, getFreeBytes()))
      return false;
    sStartupWarningShown = true;
    return true;
  }

  @NonNull
  public static String formatSize(@NonNull Context context, long bytes)
  {
    return Formatter.formatShortFileSize(context, bytes);
  }
}
