package com.idunnololz.summit.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.color.DynamicColors
import com.idunnololz.summit.R
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingsGenericBinding
import com.idunnololz.summit.databinding.FragmentSettingsThemeBinding
import com.idunnololz.summit.databinding.RadioGroupOptionSettingItemBinding
import com.idunnololz.summit.databinding.RadioGroupTitleSettingItemBinding
import com.idunnololz.summit.databinding.SettingColorItemBinding
import com.idunnololz.summit.databinding.SettingItemOnOffBinding
import com.idunnololz.summit.databinding.SettingTextValueBinding
import com.idunnololz.summit.databinding.SubgroupSettingItemBinding
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.preferences.BaseTheme
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.GlobalFontColorId
import com.idunnololz.summit.preferences.GlobalFontSizeId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.ColorSettingItem
import com.idunnololz.summit.settings.OnOffSettingItem
import com.idunnololz.summit.settings.PreferencesViewModel
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.RadioGroupSettingItem.RadioGroupOption
import com.idunnololz.summit.settings.SettingItem
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.settings.TextOnlySettingItem
import com.idunnololz.summit.settings.TextValueSettingItem
import com.idunnololz.summit.settings.ThemeSettings
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.settings.util.bindToMultiView
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.SummitActivity
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.LinkedList
import javax.inject.Inject

@AndroidEntryPoint
class SettingsThemeFragment : BaseFragment<FragmentSettingsGenericBinding>() {

  private val args: SettingsThemeFragmentArgs by navArgs()

  private val preferencesViewModel: PreferencesViewModel by viewModels()

  lateinit var preferences: Preferences

  @Inject
  lateinit var themeManager: ThemeManager

  @Inject
  lateinit var settings: ThemeSettings

  @Inject
  lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

  @Inject
  lateinit var globalStateStorage: GlobalStateStorage

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    preferences = preferencesViewModel.getPreferences(args.account)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentSettingsGenericBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    runOnReady {
      setup()
    }
  }

  private fun setup() {
    if (!isBindingAvailable()) return

    val context = requireContext()
    val parentActivity = requireSummitActivity()

    val account = args.account
    requireSummitActivity().apply {
      setupForFragment<SettingsFragment>(animate = false)

      insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

      setSupportActionBar(binding.toolbar)

      supportActionBar?.setDisplayShowHomeEnabled(true)
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      supportActionBar?.title = settings.getPageName(context)
      supportActionBar?.subtitle = account?.fullName
    }

    with(binding) {

      val adapter = SettingsAdapter(globalStateStorage)
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(true)
      recyclerView.adapter = adapter

      val data = listOf(
        SettingModelItem.SubgroupItem(
          SubgroupItem(getString(R.string.base_theme), listOf(), listOf()),
          listOf(
            settings.baseTheme.asRadioGroup(
              getCurrentValue = {
                when (preferences.baseTheme) {
                  BaseTheme.UseSystem -> R.id.setting_option_use_system
                  BaseTheme.Light -> R.id.setting_option_light_theme
                  BaseTheme.Dark -> R.id.setting_option_dark_theme
                }
              },
              onValueChanged = {
                when (it) {
                  R.id.setting_option_use_system ->
                    preferences.baseTheme = BaseTheme.UseSystem
                  R.id.setting_option_light_theme ->
                    preferences.baseTheme = BaseTheme.Light
                  R.id.setting_option_dark_theme ->
                    preferences.baseTheme = BaseTheme.Dark
                }

                binding.root.post {
                  themeManager.onPreferencesChanged()
                }
              },
            )
          )
        ),
        SettingModelItem.SubgroupItem(
          SubgroupItem(getString(R.string.theme_config), listOf(), listOf()),
          listOf(
            settings.materialYou.asOnOffSwitch(
              { preferences.isUseMaterialYou },
              {
                if (DynamicColors.isDynamicColorAvailable()) {
                  preferences.isUseMaterialYou = it
                  preferences.colorScheme = ColorSchemes.Default
                  themeManager.onPreferencesChanged()
                } else if (it) {
                  OldAlertDialogFragment.Builder()
                    .setMessage(R.string.error_dynamic_color_not_supported)
                    .createAndShow(childFragmentManager, "Asdfff")
                  preferences.isUseMaterialYou = false
                } else {
                  preferences.isUseMaterialYou = false
                }
              },
            ),
            settings.colorScheme.asCustomItem(
              { preferences.colorScheme },
              {
                ColorSchemePickerDialogFragment.newInstance(account)
                  .show(childFragmentManager, "asdaa")
              },
            )
          )
        ),
        SettingModelItem.SubgroupItem(
          SubgroupItem(getString(R.string.dark_theme_settings), listOf(), listOf()),
          listOf(
            settings.blackTheme.asOnOffSwitch(
              { preferences.isBlackTheme },
              {
                preferences.isBlackTheme = it
                themeManager.onPreferencesChanged()
              },
            ),
            settings.lessDarkBackgroundTheme.asOnOffSwitch(
              { preferences.useLessDarkBackgroundTheme },
              {
                preferences.useLessDarkBackgroundTheme = it
                themeManager.onPreferencesChanged()
              },
            )
          )
        ),
        SettingModelItem.SubgroupItem(
          SubgroupItem(getString(R.string.font_style), listOf(), listOf()),
          listOf(
            settings.font.asCustomItem(
              { preferences.globalFont },
              {
                FontPickerDialogFragment.newInstance(account)
                  .show(childFragmentManager, "FontPickerDialogFragment")
              },
            ),
            settings.fontSize.asSingleChoiceSelectorItem(
              activity = parentActivity,
              {
                preferences.globalFontSize
              },
              {
                preferences.globalFontSize = it
                themeManager.onPreferencesChanged()
              },
            ),
            settings.fontColor.asSingleChoiceSelectorItem(
              activity = parentActivity,
              {
                preferences.globalFontColor
              },
              {
                preferences.globalFontColor = it
                themeManager.onPreferencesChanged()
              },
            )
          )
        ),
        SettingModelItem.SubgroupItem(
          SubgroupItem(getString(R.string.vote_colors), listOf(), listOf()),
          listOf(
            settings.upvoteColor.asColorItem(
              { preferences.upvoteColor },
              {
                preferences.upvoteColor = it
                postAndCommentViewBuilder.onPreferencesChanged()
              },
              { context.getColorCompat(R.color.upvoteColor) },
            ),
            settings.downvoteColor.asColorItem(
              { preferences.downvoteColor },
              {
                preferences.downvoteColor = it
                postAndCommentViewBuilder.onPreferencesChanged()
              },
              { context.getColorCompat(R.color.downvoteColor) },
            ),
            BasicSettingItem(
              null,
              context.getString(R.string.swap_colors),
              null,
            ).asCustomItem(
              {
                val upvoteColor = preferences.downvoteColor
                val downvoteColor = preferences.upvoteColor

                preferences.upvoteColor = upvoteColor
                preferences.downvoteColor = downvoteColor

                postAndCommentViewBuilder.onPreferencesChanged()
              }
            )
          )
        ),
        SettingModelItem.SubgroupItem(
          SubgroupItem(getString(R.string.misc), listOf(), listOf()),
          listOf(
            settings.transparentNotificationBar.asOnOffSwitch(
              { preferences.transparentNotificationBar },
              {
                preferences.transparentNotificationBar = it
                getMainActivity()?.onPreferencesChanged()
              },
            )
          )
        ),
      )

      adapter.setData(data)
    }
  }

  sealed interface SettingModelItem {
    val setting: SettingItem

    class RadioGroupItem(
      override val setting: RadioGroupSettingItem,
      val getCurrentValue: () -> Int,
      val onValueChanged: (Int) -> Unit,
    ): SettingModelItem

    data class SubgroupItem(
      override val setting: com.idunnololz.summit.settings.SubgroupItem,
      val settings: List<SettingModelItem>,
    ) : SettingModelItem

    data class OnOffSwitchItem(
      override val setting: OnOffSettingItem,
      val getCurrentValue: () -> Boolean,
      val onValueChanged: (Boolean) -> Unit,
    ): SettingModelItem

    data class ColorItem(
      override val setting: ColorSettingItem,
      val defaultValue: Int,
      val getCurrentValue: () -> Int,
      val onValueChanged: (Int) -> Unit,
    ): SettingModelItem

    data class CustomItem(
      override val setting: SettingItem,
      val title: String,
      val description: String?,
      val getCurrentValue: () -> String?,
      val onValueChanged: () -> Unit,
    ): SettingModelItem
  }

  fun RadioGroupSettingItem.asRadioGroup(
    getCurrentValue: () -> Int,
    onValueChanged: (Int) -> Unit,
  ): SettingModelItem.RadioGroupItem {
    return SettingModelItem.RadioGroupItem(
      this,
      getCurrentValue,
      onValueChanged,
    )
  }

  fun RadioGroupSettingItem.asCustomItem(
    getCurrentValue: () -> Int,
    onValueChanged: (RadioGroupSettingItem) -> Unit,
  ): SettingModelItem.CustomItem {
    return SettingModelItem.CustomItem(
      this,
      this.title,
      this.description,
      { this.options.firstOrNull { it.id == getCurrentValue() }?.title },
      { onValueChanged(this) },
    )
  }

  fun BasicSettingItem.asCustomItem(
    onValueChanged: (BasicSettingItem) -> Unit,
  ): SettingModelItem.CustomItem {
    return SettingModelItem.CustomItem(
      this,
      this.title,
      this.description,
      { null },
      { onValueChanged(this) },
    )
  }

  fun RadioGroupSettingItem.asSingleChoiceSelectorItem(
    activity: SummitActivity,
    getCurrentValue: () -> Int,
    onValueChanged: (Int) -> Unit,
  ): SettingModelItem.CustomItem =
    asCustomItem(
      getCurrentValue,
      {
        val curChoice = getCurrentValue()
        val bottomMenu = BottomMenu(activity)
          .apply {
            val idToChoice = mutableMapOf<Int, Int>()
            options.withIndex().forEach { (index, option) ->
              idToChoice[index] = option.id
              addItem(index, option.title)

              if (curChoice == option.id) {
                setChecked(index)
              }
            }

            setTitle(title)

            setOnMenuItemClickListener {
              onValueChanged(requireNotNull(idToChoice[it.id]))
            }
          }
        activity.showBottomMenu(bottomMenu)
      }
    )

  fun OnOffSettingItem.asOnOffSwitch(
    getCurrentValue: () -> Boolean,
    onValueChanged: (Boolean) -> Unit,
  ): SettingModelItem {
    return SettingModelItem.OnOffSwitchItem(
      this,
      getCurrentValue,
      onValueChanged,
    )
  }

  fun ColorSettingItem.asColorItem(
    getCurrentValue: () -> Int,
    onValueChanged: (Int) -> Unit,
    defaultColor: () -> Int,
  ): SettingModelItem {
    return SettingModelItem.ColorItem(
      this,
      defaultColor(),
      getCurrentValue,
      onValueChanged,
    )
  }


  private class SettingsAdapter(
    private val globalStateStorage: GlobalStateStorage,
  ) : Adapter<ViewHolder>() {

    sealed interface Item {
      val setting: SettingItem

      data class RadioGroupOptionItem(
        override val setting: RadioGroupSettingItem,
        val currentValue: Int,
        val option: RadioGroupOption,
      ): Item

      data class SubtitleItem(
        override val setting: SubgroupItem,
      ): Item

      data class OnOffSwitchItem(
        override val setting: OnOffSettingItem,
        val currentValue: Boolean,
      ): Item

      data class ColorItem(
        override val setting: ColorSettingItem,
        val currentValue: Int,
        val defaultValue: Int,
      ): Item

      data class CustomItem(
        override val setting: SettingItem,
        val title: String,
        val description: String?,
        val currentValue: String?,
      ): Item
    }

    private var data: List<SettingModelItem> = listOf()

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class && old.setting.id == new.setting.id
      }
    ).apply {
      addItemType(Item.RadioGroupOptionItem::class, RadioGroupOptionSettingItemBinding::inflate) { item, b, h ->
        val setting = item.setting
        val option = item.option

        b.title.text = option.title

        b.radioButton.isChecked = option.id == item.currentValue

        if (option.icon != null) {
          b.title.setCompoundDrawablesRelativeWithIntrinsicBounds(option.icon, 0, 0, 0)
        } else {
          b.title.setCompoundDrawables(null, null, null, null)
        }

        if (option.description != null) {
          b.desc.visibility = View.VISIBLE
          b.desc.text = option.description
        } else {
          b.desc.visibility = View.GONE
        }

        b.radioButton.setOnCheckedChangeListener { _, value ->
          findSettingModel<SettingModelItem.RadioGroupItem>(setting.id)
            ?.onValueChanged
            ?.invoke(option.id)

          refreshItems()
        }
      }
      addItemType(Item.SubtitleItem::class, SubgroupSettingItemBinding::inflate) { item, b, h ->
        b.title.text = item.setting.title
      }
      addItemType(Item.OnOffSwitchItem::class, SettingItemOnOffBinding::inflate) { item, b, h ->
        item.setting.bindTo(
          b = b,
          getCurrentValue = { item.currentValue },
          onValueChanged = {
            findSettingModel<SettingModelItem.OnOffSwitchItem>(item.setting.id)
              ?.onValueChanged
              ?.invoke(it)

            refreshItems()
          }
        )
      }
      addItemType(Item.ColorItem::class, SettingColorItemBinding::inflate) { item, b, h ->
        item.setting.bindTo(
          b = b,
          globalStateStorage = globalStateStorage,
          getCurrentValue = { item.currentValue },
          onValueChanged = {
            findSettingModel<SettingModelItem.ColorItem>(item.setting.id)
              ?.onValueChanged
              ?.invoke(it)
            refreshItems()
          },
          defaultValue = { item.defaultValue },
        )
      }
      addItemType(Item.CustomItem::class, SettingTextValueBinding::inflate) { item, b, h ->
        b.title.text = item.title

        if (item.description.isNullOrBlank()) {
          b.desc.visibility = View.GONE
        } else {
          b.desc.text = item.description
          b.desc.visibility = View.VISIBLE
        }

        if (item.currentValue.isNullOrBlank()) {
          b.value.visibility = View.GONE
        } else {
          b.value.text = item.currentValue
          b.value.visibility = View.VISIBLE
        }

        b.root.tag = this
        b.root.setOnClickListener {
          findSettingModel<SettingModelItem.CustomItem>(item.setting.id)
            ?.onValueChanged
            ?.invoke()
          refreshItems()
        }
      }
    }

    private inline fun <reified T : SettingModelItem> findSettingModel(id: Int): T? {
      val data = LinkedList(data)
      while(data.isNotEmpty()) {
        val d = data.removeFirst()
        if (d is SettingModelItem.SubgroupItem) {
          data.addAll(d.settings)
        }
        if (d.setting.id == id) {
          return d as? T
        }
      }
      return null
    }

    override fun getItemViewType(position: Int): Int =
      adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int =
      adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    fun setData(data: List<SettingModelItem>) {
      this.data = data

      refreshItems()
    }

    private fun refreshItems() {
      val data = data
      val newItems = mutableListOf<Item>()

      fun processItem(item: SettingModelItem) {
        when (item) {
          is SettingModelItem.RadioGroupItem -> {
            item.setting.options.forEach {
              newItems += Item.RadioGroupOptionItem(item.setting, item.getCurrentValue(), it)
            }
          }
          is SettingModelItem.SubgroupItem -> {
            newItems += Item.SubtitleItem(
              item.setting
            )
            item.settings.forEach {
              processItem(it)
            }
          }
          is SettingModelItem.OnOffSwitchItem -> {
            newItems += Item.OnOffSwitchItem(item.setting, item.getCurrentValue())
          }
          is SettingModelItem.ColorItem -> {
            newItems += Item.ColorItem(item.setting, item.getCurrentValue(), item.defaultValue)
          }
          is SettingModelItem.CustomItem -> {
            newItems += Item.CustomItem(
              setting = item.setting,
              title = item.title,
              description = item.description,
              currentValue = item.getCurrentValue()
            )
          }
        }
      }

      data.forEach { item ->
        processItem(item)
      }

      adapterHelper.setItems(newItems, this)
    }

  }
}