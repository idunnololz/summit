package com.idunnololz.summit.settings.gestures

import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.GestureSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asColorItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import com.idunnololz.summit.settings.util.asSingleChoiceSelectorItem
import com.idunnololz.summit.settings.util.asSliderItem
import com.idunnololz.summit.util.ext.getColorCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsGesturesFragment :
  BaseSettingsFragment() {

  @Inject
  lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

  @Inject
  override lateinit var settings: GestureSettings

  override fun generateData(): List<SettingModelItem> {
    val context = requireContext()

    return listOf(
      settings.useGestureActions.asOnOffSwitch(
        { preferences.useGestureActions },
        {
          preferences.useGestureActions = it
          preferences.hideCommentActions = preferences.useGestureActions

          postAndCommentViewBuilder.onPreferencesChanged()
        },
      ),
      settings.gestureSwipeDirection.asSingleChoiceSelectorItem(
        { preferences.gestureSwipeDirection },
        { preferences.gestureSwipeDirection = it },
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.post_actions),
        listOf(
          settings.postGestureAction1.asSingleChoiceSelectorItem(
            { preferences.postGestureAction1 },
            { preferences.postGestureAction1 = it },
          ),
          settings.postGestureActionColor1.asColorItem(
            { preferences.postGestureActionColor1 },
            { preferences.postGestureActionColor1 = it },
            { context.getColorCompat(R.color.style_red) },
          ),
          settings.postGestureAction2.asSingleChoiceSelectorItem(
            { preferences.postGestureAction2 },
            { preferences.postGestureAction2 = it },
          ),
          settings.postGestureActionColor2.asColorItem(
            { preferences.postGestureActionColor2 },
            { preferences.postGestureActionColor2 = it },
            { context.getColorCompat(R.color.style_blue) },
          ),
          settings.postGestureAction3.asSingleChoiceSelectorItem(
            { preferences.postGestureAction3 },
            { preferences.postGestureAction3 = it },
          ),
          settings.postGestureActionColor3.asColorItem(
            { preferences.postGestureActionColor3 },
            { preferences.postGestureActionColor3 = it },
            { context.getColorCompat(R.color.style_amber) },
          ),
          settings.postGestureSize.asSliderItem(
            { preferences.postGestureSize },
            { preferences.postGestureSize = it },
          ),
        ),
      ),
      SettingModelItem.SubgroupItem(
        getString(R.string.comment_actions),
        listOf(
          settings.commentGestureAction1.asSingleChoiceSelectorItem(
            { preferences.commentGestureAction1 },
            { preferences.commentGestureAction1 = it },
          ),
          settings.commentGestureActionColor1.asColorItem(
            { preferences.commentGestureActionColor1 },
            {
              preferences.commentGestureActionColor1 = it
            },
            { context.getColorCompat(R.color.style_red) },
          ),
          settings.commentGestureAction2.asSingleChoiceSelectorItem(
            { preferences.commentGestureAction2 },
            { preferences.commentGestureAction2 = it },
          ),
          settings.commentGestureActionColor2.asColorItem(
            { preferences.commentGestureActionColor2 },
            {
              preferences.commentGestureActionColor2 = it
            },
            { context.getColorCompat(R.color.style_blue) },
          ),
          settings.commentGestureAction3.asSingleChoiceSelectorItem(
            { preferences.commentGestureAction3 },
            { preferences.commentGestureAction3 = it },
          ),
          settings.commentGestureActionColor3.asColorItem(
            { preferences.commentGestureActionColor3 },
            {
              preferences.commentGestureActionColor3 = it
            },
            { context.getColorCompat(R.color.style_amber) },
          ),
          settings.commentGestureSize.asSliderItem(
            { preferences.commentGestureSize },
            { preferences.commentGestureSize = it },
          ),
        ),
      ),
    )
  }
}
