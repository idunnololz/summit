package com.idunnololz.summit.emoji

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogEmojiBinding
import com.idunnololz.summit.databinding.EmojiListEmojiItemBinding
import com.idunnololz.summit.util.GridAutofitLayoutManager
import com.idunnololz.summit.util.GridSpaceItemDecoration
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EmojiPopupWindow @AssistedInject constructor(
    private val textEmojisManager: TextEmojisManager,
    @Assisted context: Context,
    @Assisted private val lifecycleOwner: LifecycleOwner,
    @Assisted private val fragmentManager: FragmentManager,
    @Assisted private val onEmojiSelected: (String) -> Unit = {},
) : PopupWindow(context) {

    @AssistedFactory
    interface Factory {
        fun create(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            fragmentManager: FragmentManager,
            onEmojiSelected: (String) -> Unit = {},
        ): EmojiPopupWindow
    }

    val binding: DialogEmojiBinding
    val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        width = ViewGroup.LayoutParams.MATCH_PARENT

        val binding = DialogEmojiBinding.inflate(LayoutInflater.from(context)).also {
            binding = it
        }
        setContentView(binding.root)

        val adapter = EmojisAdapter {
            onEmojiSelected(it)
            dismiss()
        }

        binding.recyclerView.apply {
            this.adapter = adapter
            layoutManager = GridAutofitLayoutManager(context, context.resources.getDimensionPixelSize(R.dimen.emoji_item_width))
            addItemDecoration(
                GridSpaceItemDecoration(
                    space = context.resources.getDimensionPixelSize(R.dimen.padding_half),
                    spaceAboveFirstAndBelowLastItem = false,
                    horizontalSpaceOnFirstAndLastItem = false,
                ),
            )
        }
        binding.settings.setOnClickListener {
            EmojiPopupEditorDialogFragment.show(fragmentManager)
        }

        setBackgroundDrawable(null)

        binding.root.setOnClickListener {
            dismiss()
        }
        isOutsideTouchable = true

        lifecycleOwner.lifecycleScope.launch {
            val allOptions = textEmojisManager.getAllEmojis()

            adapter.setItems(allOptions.map { EmojisAdapter.Item.EmojiItem(it.text) }) {
                update()
            }
        }

        val onEmojiChangedJob = lifecycleOwner.lifecycleScope.launch {
            textEmojisManager.emojisChangedFlow.collect {
                lifecycleOwner.lifecycleScope.launch {
                    val allOptions = textEmojisManager.getAllEmojis()

                    adapter.setItems(allOptions.map { EmojisAdapter.Item.EmojiItem(it.text) }) {
                        update()
                    }
                }
            }
        }

        setOnDismissListener {
            onEmojiChangedJob.cancel()
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    fun showOnView(anchor: View) {
        val anchorPos = IntArray(2).apply {
            anchor.getLocationOnScreen(this)
        }
        val visibleDisplayFrame = Rect()
        var bottomInset = 0
        val rootView = anchor.rootView
        requireNotNull(rootView).apply {
            getWindowVisibleDisplayFrame(visibleDisplayFrame)
            bottomInset = rootView.height - visibleDisplayFrame.bottom
        }
        val yOffset = anchorPos[1] + anchor.paddingTop

        val distanceToBottom = visibleDisplayFrame.bottom - yOffset
        val distanceToTop: Int = yOffset - visibleDisplayFrame.top

//        showAsDropDown(anchor)

        if (distanceToBottom > distanceToTop) {
//            showAsDropDown(anchor, 0, 0)
        } else {
//            showAtLocation(anchor, Gravity.TOP, 0, 0)
            showAsDropDown(anchor, 0, -distanceToTop, Gravity.TOP or Gravity.START)
        }

        binding.root.post {
            apply {
                update()
            }
        }
    }

    private class EmojisAdapter(
        private val onEmojiSelected: (String) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data class EmojiItem(
                val text: String,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                is Item.EmojiItem ->
                    old.text == (new as Item.EmojiItem).text
            }
        }).apply {
            addItemType(Item.EmojiItem::class, EmojiListEmojiItemBinding::inflate) { item, b, h ->
                b.text.text = item.text
                b.root.setOnClickListener {
                    onEmojiSelected(item.text)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setItems(items: List<Item>, cb: () -> Unit) {
            adapterHelper.setItems(items, this) { cb() }
        }
    }
}
