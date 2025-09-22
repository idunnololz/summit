package com.idunnololz.summit.emoji

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.DialogFragmentEmojiPopupEditorBinding
import com.idunnololz.summit.databinding.EmojiEditorEmojiItemBinding
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.GridAutofitLayoutManager
import com.idunnololz.summit.util.GridSpaceItemDecoration
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class EmojiPopupEditorDialogFragment :
  BaseDialogFragment<DialogFragmentEmojiPopupEditorBinding>(),
  FullscreenDialogFragment {

  companion object {
    const val REQUEST_KEY = "DraftsDialogFragment_req_key"
    const val REQUEST_KEY_RESULT = "result"

    fun show(fragmentManager: FragmentManager) {
      EmojiPopupEditorDialogFragment()
        .apply {}
        .showAllowingStateLoss(fragmentManager, "EmojiPopupEditorDialogFragment")
    }
  }

  private val viewModel: EmojiPopupEditorViewModel by viewModels()

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  private val resetEmojisDialogLauncher = newAlertDialogLauncher("reset_emojis") {
    if (it.isOk) {
      viewModel.resetEmojis()
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

    setBinding(DialogFragmentEmojiPopupEditorBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      childFragmentManager.setFragmentResultListener(
        TextEmojiEditDialogFragment.REQUEST_KEY,
        viewLifecycleOwner,
      ) { requestKey, result ->
        val result = TextEmojiEditDialogFragment.getResult(result)

        if (result != null) {
          viewModel.addOrUpdateTextEmoji(
            result.id,
            result.textEmoji,
            result.delete,
          )
        }
      }

      requireMainActivity().apply {
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.root)
      }

      toolbar.apply {
        setTitle(R.string.manage_text_emojis)
        setNavigationIcon(R.drawable.baseline_close_24)
        setNavigationOnClickListener {
          dismiss()
        }
        setNavigationIconTint(
          context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
        )

        inflateMenu(R.menu.menu_emoji_popup_editor)
        setOnMenuItemClickListener {
          when (it.itemId) {
            R.id.reset -> {
              resetEmojisDialogLauncher.launchDialog {
                messageResId = R.string.warn_reset_emojis
                positionButtonResId = R.string.reset_emojis
                negativeButtonResId = android.R.string.cancel
              }
            }
          }
          true
        }
      }

      val adapter = EditEmojisAdapter(context) { id, textEmoji ->
        TextEmojiEditDialogFragment.show(childFragmentManager, textEmoji, id)
      }

      recyclerView.layoutManager = GridAutofitLayoutManager(
        context,
        Utils.convertDpToPixel(200f).toInt(),
      )
      recyclerView.setHasFixedSize(true)
      recyclerView.adapter = adapter
      recyclerView.addItemDecoration(
        GridSpaceItemDecoration(
          space = context.resources.getDimensionPixelSize(R.dimen.padding_half),
          spaceAboveFirstAndBelowLastItem = true,
          horizontalSpaceOnFirstAndLastItem = true,
        ),
      )

      val callback: ItemTouchHelper.Callback = ItemMoveCallback(adapter)
      val touchHelper = ItemTouchHelper(callback)
      touchHelper.attachToRecyclerView(recyclerView)

      fab.setOnClickListener {
        TextEmojiEditDialogFragment.show(childFragmentManager, "", 0)
      }

      viewModel.loadData()

      viewModel.model.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            loadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading -> {
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            loadingView.hideAll()

            adapter.setData(it.data.emojis)
          }
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()

    commitChanges()
  }

  private fun commitChanges() {
    val adapter = binding.recyclerView.adapter as? EditEmojisAdapter
    if (adapter != null) {
      viewModel.commitChanges(adapter.getEntries().map { it.entry })
    }
  }

  class ItemMoveCallback(
    private val mAdapter: ItemTouchHelperContract,
  ) : ItemTouchHelper.Callback() {
    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun onSwiped(viewHolder: ViewHolder, i: Int) {
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
      val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
        ItemTouchHelper.START or ItemTouchHelper.END
      return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
      recyclerView: RecyclerView,
      viewHolder: ViewHolder,
      target: ViewHolder,
    ): Boolean {
      mAdapter.onRowMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
      return true
    }

    override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
      if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
        mAdapter.onRowSelected(viewHolder)
      }

      super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
      super.clearView(recyclerView, viewHolder)

      mAdapter.onRowClear(viewHolder)
    }

    interface ItemTouchHelperContract {
      fun onRowMoved(fromPosition: Int, toPosition: Int)
      fun onRowSelected(myViewHolder: ViewHolder?)
      fun onRowClear(myViewHolder: ViewHolder?)
    }
  }

  private class EditEmojisAdapter(
    private val context: Context,
    private val onEditClick: (id: Long, textEmoji: String) -> Unit,
  ) : Adapter<ViewHolder>(),
    ItemMoveCallback.ItemTouchHelperContract {

    sealed interface Item {
      data class Emoji(
        val isModifiable: Boolean,
        val emoji: String,
        val id: Long,
      ) : Item
    }

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class &&
          when (old) {
            is Item.Emoji -> old.emoji == (new as Item.Emoji).emoji
          }
      },
    )

    private var data: List<TextEmoji> = listOf()

    init {
      adapterHelper.apply {
        addItemType(Item.Emoji::class, EmojiEditorEmojiItemBinding::inflate) { item, b, h ->
          b.text.text = item.emoji
          b.root.setOnClickListener {
            onEditClick(item.id, item.emoji)
          }
        }
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    fun setData(emojis: List<TextEmoji>) {
      data = emojis
      refresh()
    }

    private fun refresh() {
      adapterHelper.setItems(
        data.map {
          Item.Emoji(
            isModifiable = it is TextEmoji,
            emoji = it.text,
            id = it.id,
          )
        },
        this,
      )
    }

    fun getEntries() = data

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
      val mutableItems = data.toMutableList()

      if (fromPosition < toPosition) {
        for (i in fromPosition until toPosition) {
          Collections.swap(mutableItems, i, i + 1)
        }
      } else {
        for (i in fromPosition downTo toPosition + 1) {
          Collections.swap(mutableItems, i, i - 1)
        }
      }
      data = mutableItems
      notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {
    }

    override fun onRowClear(myViewHolder: ViewHolder?) {
    }
  }
}
