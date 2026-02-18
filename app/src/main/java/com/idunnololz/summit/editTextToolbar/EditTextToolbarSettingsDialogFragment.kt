package com.idunnololz.summit.editTextToolbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.DialogFragmentEditTextToolbarSettingsBinding
import com.idunnololz.summit.databinding.TextFieldToolbarOptionItemBinding
import com.idunnololz.summit.databinding.TextFieldToolbarShowLabelsItemBinding
import com.idunnololz.summit.preferences.TextFieldToolbarSettings
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditTextToolbarSettingsDialogFragment :
  BaseDialogFragment<DialogFragmentEditTextToolbarSettingsBinding>(),
  FullscreenDialogFragment {

  companion object {
    fun show(childFragmentManager: FragmentManager) {
      EditTextToolbarSettingsDialogFragment()
        .show(childFragmentManager, "EditTextToolbarSettingsDialogFragment")
    }
  }

  @Inject
  lateinit var textFieldToolbarManager: TextFieldToolbarManager

  private var adapter: ReorderableListAdapter? = null

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  private val unsavedChangesDialogLauncher = newAlertDialogLauncher("unsaved_changes") {
    if (it.isOk) {
      dismiss()
    }
  }

  private val unsavedChangesBackPressedHandler = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      unsavedChangesDialogLauncher.launchDialog {
        titleResId = R.string.error_unsaved_changes
        messageResId = R.string.error_multi_community_unsaved_changes
        positionButtonResId = R.string.proceed_anyways
        negativeButtonResId = R.string.cancel
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.let { window ->
      WindowCompat.setDecorFitsSystemWindows(window, false)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentEditTextToolbarSettingsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireMainActivity().apply {
      insetViewAutomaticallyByMargins(viewLifecycleOwner, binding.root)
    }

    with(binding) {
      toolbar.title = getString(R.string.text_field_toolbar_settings)
      toolbar.setNavigationIcon(R.drawable.baseline_close_24)
      toolbar.setNavigationIconTint(
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
      )
      toolbar.setNavigationOnClickListener {
        onBackPressedDispatcher.onBackPressed()
      }

      val textFieldToolbarSettings = textFieldToolbarManager.textFieldToolbarSettings.value
        ?: TextFieldToolbarSettings()
      val shownOptions = textFieldToolbarSettings.toolbarOptions
      val data = ToolbarData(
        showLabels = textFieldToolbarSettings.showLabels,
        options = shownOptions.map {
          TextFieldToolbarOptionData(
            option = it,
            visible = !textFieldToolbarSettings.hiddenOptions.contains(it),
          )
        } + TextFieldToolbarOption.entries
          .filter { !shownOptions.contains(it) }
          .map {
            TextFieldToolbarOptionData(
              option = it,
              visible = false,
            )
          },
      )

      val adapter = ReorderableListAdapter(data)
      this@EditTextToolbarSettingsDialogFragment.adapter = adapter
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.adapter = adapter
      recyclerView.setup(animationsHelper)
      recyclerView.setHasFixedSize(true)

      adapter.setRecyclerView(recyclerView)

      neutralButton.setOnClickListener {
        textFieldToolbarManager.updateToolbarSettings(null)
        dismiss()
      }
      negativeButton.setOnClickListener {
        dismiss()
      }
      positiveButton.setOnClickListener {
        val currentSettings = textFieldToolbarManager.textFieldToolbarSettings.value
          ?: TextFieldToolbarSettings()
        textFieldToolbarManager.updateToolbarSettings(
          currentSettings.copy(
            useCustomToolbar = true,
            toolbarOptions = adapter.items
              .filterIsInstance<Item.ToolbarItem>()
              .map { it.option.option },
            hiddenOptions = adapter.items
              .filterIsInstance<Item.ToolbarItem>()
              .filter { !it.option.visible }
              .map { it.option.option },
            showLabels = adapter.items
              .filterIsInstance<Item.ShowLabelsItem>()
              .firstOrNull()
              ?.showLabels == true
          ),
        )
        dismiss()
      }

      viewLifecycleOwner.lifecycleScope.launch {
        adapter.changed.collect { unsavedChanges ->
          unsavedChangesBackPressedHandler.isEnabled = unsavedChanges
        }
      }
    }

    onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      unsavedChangesBackPressedHandler,
    )
  }

  sealed interface Item {
    data class ToolbarItem(
      val option: TextFieldToolbarOptionData,
    ) : Item

    data class ShowLabelsItem(
      val showLabels: Boolean,
    ) : Item
  }

  data class ToolbarData(
    val showLabels: Boolean,
    val options: List<TextFieldToolbarOptionData>,
  )

  data class TextFieldToolbarOptionData(
    val option: TextFieldToolbarOption,
    val visible: Boolean,
  )

  private class ReorderableListAdapter(
    private val data: ToolbarData,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<Item> = listOf()
    var changed = MutableStateFlow(false)
    private val ith: ItemTouchHelper

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class &&
          when (old) {
            is Item.ToolbarItem ->
              old == new

            is Item.ShowLabelsItem -> true
          }
      },
    )

    init {
      refreshItems()

      val callback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0,
      ) {

        override fun onMove(
          recyclerView: RecyclerView,
          viewHolder: RecyclerView.ViewHolder,
          target: RecyclerView.ViewHolder,
        ): Boolean {
          swap(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
          return true
        }

        override fun canDropOver(
          recyclerView: RecyclerView,
          current: RecyclerView.ViewHolder,
          target: RecyclerView.ViewHolder,
        ): Boolean = items[target.bindingAdapterPosition] is Item.ToolbarItem

        override fun isLongPressDragEnabled(): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
      }

      ith = ItemTouchHelper(callback)

      adapterHelper.apply {
        addItemType(
          Item.ShowLabelsItem::class,
          TextFieldToolbarShowLabelsItemBinding::inflate,
        ) { item, b, h ->
          b.showLabels.isChecked = item.showLabels
          b.showLabels.setOnCheckedChangeListener { _, isChecked ->
            this@ReorderableListAdapter.items = this@ReorderableListAdapter.items.map {
              when (it) {
                is Item.ShowLabelsItem -> {
                  Item.ShowLabelsItem(isChecked)
                }
                is Item.ToolbarItem -> it
              }
            }
          }
        }
        addItemType(
          Item.ToolbarItem::class,
          TextFieldToolbarOptionItemBinding::inflate,
        ) { item, b, h ->
          b.name.setCompoundDrawablesRelativeWithIntrinsicBounds(
            item.option.option.icon,
            0,
            0,
            0,
          )
          b.name.setText(item.option.option.title)
          if (item.option.visible) {
            b.visible.setImageResource(R.drawable.baseline_visibility_24)
            b.name.alpha = 1f
            b.visible.alpha = 1f
            b.dragRegion.alpha = 1f
          } else {
            b.visible.setImageResource(R.drawable.baseline_visibility_off_24)
            b.name.alpha = 0.5f
            b.visible.alpha = 0.5f
            b.dragRegion.alpha = 0.5f
          }
          b.visible.setOnClickListener {
            this@ReorderableListAdapter.items = this@ReorderableListAdapter.items.map {
              when (it) {
                is Item.ToolbarItem -> {
                  if (it.option.option == item.option.option) {
                    item.copy(
                      option = item.option.copy(
                        visible = !item.option.visible
                      )
                    )
                  } else {
                    it
                  }
                }
                else -> it
              }
            }
            adapterHelper.setItems(this@ReorderableListAdapter.items, this@ReorderableListAdapter)

            changed.value = true
          }

          b.dragRegion.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
              ith.startDrag(h)
            }
            false
          }
        }
      }
    }

    private fun refreshItems() {
      val items = mutableListOf<Item>()

      items.add(Item.ShowLabelsItem(data.showLabels))
      data.options.mapTo(items) {
        Item.ToolbarItem(it)
      }

      this.items = items

      adapterHelper.setItems(items, this)
    }

    fun setRecyclerView(v: RecyclerView) {
      ith.attachToRecyclerView(v)
    }

    private fun swap(fromPosition: Int, toPosition: Int) {
      val mutableItems = items.toMutableList()

      if (fromPosition < toPosition) {
        for (i in fromPosition until toPosition) {
          Collections.swap(mutableItems, i, i + 1)
        }
      } else {
        for (i in fromPosition downTo toPosition + 1) {
          Collections.swap(mutableItems, i, i - 1)
        }
      }
      items = mutableItems
      adapterHelper.setItems(items, this)

      changed.value = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun getItemCount(): Int = adapterHelper.itemCount
  }
}
