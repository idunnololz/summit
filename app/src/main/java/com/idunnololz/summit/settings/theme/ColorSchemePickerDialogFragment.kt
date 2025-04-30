package com.idunnololz.summit.settings.theme

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.databinding.ColorSchemeItemBinding
import com.idunnololz.summit.databinding.DialogFragmentColorSchemePickerBinding
import com.idunnololz.summit.preferences.ColorSchemeId
import com.idunnololz.summit.preferences.ColorSchemes
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.settings.PreferencesViewModel
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ColorSchemePickerDialogFragment :
  BaseBottomSheetDialogFragment<DialogFragmentColorSchemePickerBinding>(),
  FullscreenDialogFragment {

  companion object {
    fun newInstance(account: Account?) = ColorSchemePickerDialogFragment().apply {
      arguments = ColorSchemePickerDialogFragmentArgs(account)
        .toBundle()
    }
  }

  private val args: ColorSchemePickerDialogFragmentArgs by navArgs()

  private val preferencesViewModel: PreferencesViewModel by viewModels()

  lateinit var preferences: Preferences

  @Inject
  lateinit var themeManager: ThemeManager

  @Inject
  lateinit var animationsHelper: AnimationsHelper

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

    setBinding(DialogFragmentColorSchemePickerBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      recyclerView.setup(animationsHelper)
      recyclerView.setHasFixedSize(true)
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.adapter = ColorSchemeAdapter(context) {
        preferences.colorScheme = it
        preferences.isUseMaterialYou = false

        themeManager.onPreferencesChanged()

        dismiss()
      }
    }
  }

  private class ColorSchemeAdapter(
    private val context: Context,
    private val onColorSchemeSelected: (ColorSchemeId) -> Unit,
  ) : Adapter<ViewHolder>() {

    sealed interface Item {
      data class ColorSchemeItem(
        val colorSchemeId: ColorSchemeId,
        val name: String,
        @ColorInt val primaryColor: Int,
        @ColorInt val secondaryColor: Int,
        @ColorInt val tertiaryColor: Int,
      ) : Item
    }

    private val adapterHelper = AdapterHelper<Item>(
      areItemsTheSame = { old, new ->
        old::class == new::class && when (old) {
          is Item.ColorSchemeItem ->
            old.colorSchemeId == (new as Item.ColorSchemeItem).colorSchemeId
        }
      },
    ).apply {
      addItemType(
        clazz = Item.ColorSchemeItem::class,
        inflateFn = ColorSchemeItemBinding::inflate,
      ) { item, b, h ->
        b.title.text = item.name
        b.color1.background = ColorDrawable(item.primaryColor)
        b.color2.background = ColorDrawable(item.secondaryColor)
        b.color3.background = ColorDrawable(item.tertiaryColor)

        b.root.setOnClickListener {
          onColorSchemeSelected(item.colorSchemeId)
        }
      }
    }

    init {
      refreshItems()
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    private fun refreshItems() {
      val newItems = listOf(
        Item.ColorSchemeItem(
          ColorSchemes.Default,
          context.getString(R.string._default),
          context.getColorCompat(R.color.colorPrimary),
          context.getColorCompat(R.color.colorSecondary),
          context.getColorCompat(R.color.colorTertiary),
        ),
        Item.ColorSchemeItem(
          ColorSchemes.Blue,
          context.getString(R.string.blue),
          context.getColorCompat(R.color.theme_blue_md_theme_light_primary),
          context.getColorCompat(R.color.theme_blue_md_theme_light_secondary),
          context.getColorCompat(R.color.theme_blue_md_theme_light_tertiary),
        ),
        Item.ColorSchemeItem(
          ColorSchemes.Red,
          context.getString(R.string.red),
          context.getColorCompat(R.color.theme_red_md_theme_light_primary),
          context.getColorCompat(R.color.theme_red_md_theme_light_secondary),
          context.getColorCompat(R.color.theme_red_md_theme_light_tertiary),
        ),
        Item.ColorSchemeItem(
          ColorSchemes.TalhaPurple,
          context.getString(R.string.talha_e_purple),
          context.getColorCompat(R.color.theme_talha_e_purple_md_theme_light_primary),
          context.getColorCompat(R.color.theme_talha_e_purple_md_theme_light_secondary),
          context.getColorCompat(R.color.theme_talha_e_purple_md_theme_light_tertiary),
        ),
        Item.ColorSchemeItem(
          ColorSchemes.TalhaGreen,
          context.getString(R.string.talha_e_green),
          context.getColorCompat(R.color.theme_talha_e_green_md_theme_light_primary),
          context.getColorCompat(R.color.theme_talha_e_green_md_theme_light_secondary),
          context.getColorCompat(R.color.theme_talha_e_green_md_theme_light_tertiary),
        ),
        Item.ColorSchemeItem(
          ColorSchemes.TalhaPink,
          context.getString(R.string.talha_e_pink),
          context.getColorCompat(R.color.theme_talha_e_pink_md_theme_light_primary),
          context.getColorCompat(R.color.theme_talha_e_pink_md_theme_light_secondary),
          context.getColorCompat(R.color.theme_talha_e_pink_md_theme_light_tertiary),
        ),
        Item.ColorSchemeItem(
          ColorSchemes.Peachie,
          context.getString(R.string.peachie),
          context.getColorCompat(R.color.theme_peachie_md_theme_light_primary),
          context.getColorCompat(R.color.theme_peachie_md_theme_light_secondary),
          context.getColorCompat(R.color.theme_peachie_md_theme_light_tertiary),
        ),
        Item.ColorSchemeItem(
          ColorSchemes.Fuchsia,
          context.getString(R.string.fuchsia),
          context.getColorCompat(R.color.theme_fuchsia_md_theme_light_primary),
          context.getColorCompat(R.color.theme_fuchsia_md_theme_light_secondary),
          context.getColorCompat(R.color.theme_fuchsia_md_theme_light_tertiary),
        ),
        Item.ColorSchemeItem(
          ColorSchemes.Minty,
          context.getString(R.string.minty),
          context.getColorCompat(R.color.theme_minty_md_theme_light_primary),
          context.getColorCompat(R.color.theme_minty_md_theme_light_secondary),
          context.getColorCompat(R.color.theme_minty_md_theme_light_tertiary),
        ),
      )

      adapterHelper.setItems(newItems, this)
    }
  }
}
