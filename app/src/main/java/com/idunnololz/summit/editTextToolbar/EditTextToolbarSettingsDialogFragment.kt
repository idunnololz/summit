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
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentEditTextToolbarSettingsBinding
import com.idunnololz.summit.databinding.TextFieldToolbarOptionItemBinding
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
    FullscreenDialogFragment,
    OldAlertDialogFragment.AlertDialogFragmentListener {

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

    private val unsavedChangesBackPressedHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            OldAlertDialogFragment.Builder()
                .setTitle(R.string.error_unsaved_changes)
                .setMessage(R.string.error_multi_community_unsaved_changes)
                .setPositiveButton(R.string.proceed_anyways)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "UnsavedChanges")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
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
                options = shownOptions,
                hiddenOptions = TextFieldToolbarOption.entries.filter {
                    !shownOptions.contains(
                        it,
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
                        toolbarOptions = adapter.items.map {
                            when (it) {
                                is Item.ToolbarItem -> it.option
                            }
                        },
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
            val option: TextFieldToolbarOption,
        ) : Item
    }

    data class ToolbarData(
        val options: List<TextFieldToolbarOption>,
        val hiddenOptions: List<TextFieldToolbarOption>,
    )

    private class ReorderableListAdapter(
        private val data: ToolbarData,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var items: List<Item> = listOf()
        var changed = MutableStateFlow(false)
        private val ith: ItemTouchHelper

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    is Item.ToolbarItem ->
                        old == new
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

                override fun isLongPressDragEnabled(): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            }

            ith = ItemTouchHelper(callback)

            adapterHelper.apply {
                addItemType(
                    Item.ToolbarItem::class,
                    TextFieldToolbarOptionItemBinding::inflate,
                ) { item, b, h ->
                    b.name.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        item.option.icon,
                        0,
                        0,
                        0,
                    )
                    b.name.setText(item.option.title)

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
            items = data.options.map {
                Item.ToolbarItem(it)
            } + data.hiddenOptions.map {
                Item.ToolbarItem(it)
            }

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

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        dismiss()
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
