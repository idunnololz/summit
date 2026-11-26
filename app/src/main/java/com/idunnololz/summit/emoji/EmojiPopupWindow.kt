package com.idunnololz.summit.emoji

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogEmojiBinding
import com.idunnololz.summit.databinding.EmojiListInstanceEmojiItemBinding
import com.idunnololz.summit.databinding.EmojiListTextEmojiItemBinding
import com.idunnololz.summit.util.BaseDialogFragment.Companion.gestureInterpolator
import com.idunnololz.summit.util.GridAutofitLayoutManager
import com.idunnololz.summit.util.GridSpaceItemDecoration
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorStateListFromAttribute
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EmojiPopupWindow @AssistedInject constructor(
  private val textEmojisManager: TextEmojisManager,
  private val instanceEmojisManager: InstanceEmojisManager,
  @Assisted private val context: Context,
  @Assisted private val lifecycleOwner: LifecycleOwner,
  @Assisted private val fragmentManager: FragmentManager,
  @Assisted private val onEmojiSelected: (SelectedEmoji) -> Unit = {},
) : PopupWindow(context) {

  sealed interface SelectedEmoji {
    class SelectedTextEmoji(val text: String) : SelectedEmoji
    class SelectedInstanceEmoji(val emoji: InstanceEmoji) : SelectedEmoji
  }

  @AssistedFactory
  interface Factory {
    fun create(
      context: Context,
      lifecycleOwner: LifecycleOwner,
      fragmentManager: FragmentManager,
      onEmojiSelected: (SelectedEmoji) -> Unit = {},
    ): EmojiPopupWindow
  }

  val binding: DialogEmojiBinding
  val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private var onBackPressedCallback: OnBackPressedCallback? = null

  private var onEmojiChangedJob: Job? = null

  init {
    width = ViewGroup.LayoutParams.MATCH_PARENT

    val binding = DialogEmojiBinding.inflate(LayoutInflater.from(context)).also {
      binding = it
    }
    setContentView(binding.root)


    binding.toolbar.apply {
      val iconTint = this.context.getColorStateListFromAttribute(
        androidx.appcompat.R.attr.colorControlNormal,
      )
      menu.add(0, R.id.settings, 0, R.string.settings)
        .apply {
          setIcon(R.drawable.outline_settings_24)
          MenuItemCompat.setIconTintList(this, iconTint)
          setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

      setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.settings -> {
            EmojiPopupEditorDialogFragment.show(fragmentManager)
          }
        }

        true
      }
    }
    val pages = listOf(binding.textEmojisRecyclerView, binding.instanceEmojisRecyclerView)
    binding.contentContainer.removeAllViews()
    binding.contentContainer.adapter = object : PagerAdapter() {
      override fun getCount(): Int = pages.size

      override fun instantiateItem(container: ViewGroup, position: Int): Any =
        pages[position].also(container::addView)

      override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View)
      }

      override fun isViewFromObject(view: View, item: Any): Boolean = view === item
    }
    binding.contentContainer.addOnPageChangeListener(
      object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
          when (position) {
            0 -> binding.buttonGroup.check(binding.textEmojisButton.id)
            1 -> binding.buttonGroup.check(binding.instanceEmojisButton.id)
          }
        }
      },
    )
    binding.textEmojisButton.setOnClickListener {
      binding.contentContainer.currentItem = 0
    }
    binding.instanceEmojisButton.setOnClickListener {
      binding.contentContainer.currentItem = 1
    }

    setupTextEmojiList()
    setupInstanceEmojiList()

    setBackgroundDrawable(null)

    binding.root.setOnClickListener {
      dismiss()
    }
    isOutsideTouchable = true

    setOnDismissListener {
      onBackPressedCallback?.remove()
      onBackPressedCallback = null
      onEmojiChangedJob?.cancel()
    }
  }

  private fun setupInstanceEmojiList() {
    val emojiCollection = instanceEmojisManager.getAllEmojis() ?: return

    binding.instanceEmojisButton.text = emojiCollection.instance
    binding.instanceEmojisRecyclerView.apply {
      this.adapter = InstanceEmojisAdapter {
        onEmojiSelected(SelectedEmoji.SelectedInstanceEmoji(it))
        dismiss()
      }.also {
        it.setItems(emojiCollection.emojis) {}
      }
      layoutManager =
        GridAutofitLayoutManager(
          context,
          context.resources.getDimensionPixelSize(R.dimen.instance_emoji_item_width),
        )
      addItemDecoration(
        GridSpaceItemDecoration(
          space = context.resources.getDimensionPixelSize(R.dimen.padding_half),
          spaceAboveFirstAndBelowLastItem = false,
          horizontalSpaceOnFirstAndLastItem = false,
        ),
      )
    }
  }

  private fun setupTextEmojiList() {
    val adapter = TextEmojisAdapter {
      onEmojiSelected(SelectedEmoji.SelectedTextEmoji(it))
      dismiss()
    }

    binding.textEmojisRecyclerView.apply {
      this.adapter = adapter
      layoutManager =
        GridAutofitLayoutManager(
          context,
          context.resources.getDimensionPixelSize(R.dimen.text_emoji_item_width),
        )
      addItemDecoration(
        GridSpaceItemDecoration(
          space = context.resources.getDimensionPixelSize(R.dimen.padding_half),
          spaceAboveFirstAndBelowLastItem = false,
          horizontalSpaceOnFirstAndLastItem = false,
        ),
      )
    }

    lifecycleOwner.lifecycleScope.launch {
      val allOptions = textEmojisManager.getAllEmojis()

      adapter.setItems(allOptions.map { TextEmojisAdapter.Item.EmojiItem(it.text) }) {
        update()
      }
    }

    onEmojiChangedJob = lifecycleOwner.lifecycleScope.launch {
      textEmojisManager.emojisChangedFlow.collect {
        lifecycleOwner.lifecycleScope.launch {
          val allOptions = textEmojisManager.getAllEmojis()

          adapter.setItems(allOptions.map { TextEmojisAdapter.Item.EmojiItem(it.text) }) {
            update()
          }
        }
      }
    }
  }

  private fun getPredictiveBackBackPressCallback(): OnBackPressedCallback {
    val predictiveBackMargin = context.resources.getDimensionPixelSize(R.dimen.predictive_back_margin)
    var initialTouchY = -1f

    return object : OnBackPressedCallback(true) {

      private val decorView: View?
        get() = binding.root.parent as? View

      override fun handleOnBackPressed() {
        // This invokes the sharedElementReturnTransition, which is
        // MaterialContainerTransform.
        dismiss()
      }

      override fun handleOnBackStarted(backEvent: BackEventCompat) {
        super.handleOnBackStarted(backEvent)

        val background = decorView ?: return
        val focus = background.findFocus()

        focus?.clearFocus()
      }

      override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        val background = decorView ?: return
        val progress = gestureInterpolator.getInterpolation(backEvent.progress)

        if (initialTouchY < 0f) {
          initialTouchY = backEvent.touchY
        }
        val progressY = gestureInterpolator.getInterpolation(
          (backEvent.touchY - initialTouchY) / background.height,
        )

        // See the motion spec about the calculations below.
        // https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs

        // Shift horizontally.
        val maxTranslationX = (background.width / 20) - predictiveBackMargin
        background.translationX = progress * maxTranslationX *
          (if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1)

        // Shift vertically.
        val maxTranslationY = (background.height / 20) - predictiveBackMargin
        background.translationY = progressY * maxTranslationY

        // Scale down from 100% to 90%.
        val scale = 1f - (0.1f * progress)
        background.scaleX = scale
        background.scaleY = scale
      }

      override fun handleOnBackCancelled() {
        val background = decorView ?: return
        initialTouchY = -1f
        background.run {
          translationX = 0f
          translationY = 0f
          scaleX = 1f
          scaleY = 1f
        }
      }
    }
  }

  fun showOnView(anchor: View) {
    if (isShowing) return

    val onBackPressedCallback = getPredictiveBackBackPressCallback()
    anchor.findViewTreeOnBackPressedDispatcherOwner()
      ?.onBackPressedDispatcher
      ?.addCallback(lifecycleOwner, onBackPressedCallback)
    this.onBackPressedCallback = onBackPressedCallback

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
      showAsDropDown(anchor, 0, 0)
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

  private class InstanceEmojisAdapter(
    private val onEmojiSelected: (InstanceEmoji) -> Unit,
  ) : Adapter<ViewHolder>() {

    sealed interface Item {
      data class EmojiItem(
        val emoji: InstanceEmoji,
      ) : Item
    }

    private val adapterHelper = AdapterHelper<Item>(areItemsTheSame = { old, new ->
      old::class == new::class &&
        when (old) {
          is Item.EmojiItem ->
            old.emoji.id == (new as Item.EmojiItem).emoji.id
        }
    }).apply {
      addItemType(Item.EmojiItem::class, EmojiListInstanceEmojiItemBinding::inflate) { item, b, h ->
        b.image.load(item.emoji.imageUrl) {
          size(Utils.convertDpToPixel(48f).toInt(), Utils.convertDpToPixel(48f).toInt())
        }
        b.root.setOnClickListener {
          onEmojiSelected(item.emoji)
        }
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    fun setItems(items: List<InstanceEmoji>, cb: () -> Unit) {
      adapterHelper.setItems(items.map {
        Item.EmojiItem(
          it
        )
      }, this) { cb() }
    }
  }

  private class TextEmojisAdapter(
    private val onEmojiSelected: (String) -> Unit,
  ) : Adapter<ViewHolder>() {

    sealed interface Item {
      data class EmojiItem(
        val text: String,
      ) : Item
    }

    private val adapterHelper = AdapterHelper<Item>(areItemsTheSame = { old, new ->
      old::class == new::class &&
        when (old) {
          is Item.EmojiItem ->
            old.text == (new as Item.EmojiItem).text
        }
    }).apply {
      addItemType(Item.EmojiItem::class, EmojiListTextEmojiItemBinding::inflate) { item, b, h ->
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
