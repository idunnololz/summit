package com.idunnololz.summit.view.loadingIndicator;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentResolver;
import android.provider.Settings.Global;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * This is a utility class to get system animator duration scale from the system settings. It's used
 * as instances so that some requirements for testing can be met by mocking.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class AnimatorDurationScaleProvider {

  /** Returns the animator duration scale from developer options setting. */
  public float getSystemAnimatorDurationScale(@NonNull ContentResolver contentResolver) {
    return Global.getFloat(contentResolver, Global.ANIMATOR_DURATION_SCALE, 1f);
  }
}