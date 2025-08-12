package com.idunnololz.summit.util.recyclerView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.R
import com.idunnololz.summit.util.recyclerView.AdapterHelper.RuntimeItemType
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class AdapterHelper<T : Any>(
  private val areItemsTheSame: (old: T, new: T) -> Boolean,
  private val areContentsTheSame: (old: T, new: T) -> Boolean =
    { oldItem, newItem -> oldItem == newItem },
  private val getChangePayload: (old: T, new: T) -> Any? =
    { old, new -> null },
) {

  data class ItemInfo<T>(
    val viewType: Int,
    val viewBindingClass: KClass<out ViewBinding>,
    val inflateFn: (LayoutInflater, ViewGroup, Boolean) -> ViewBinding,
    val bindViewHolder: (item: T, b: ViewBinding, h: ViewHolder) -> Unit,
    val onViewCreated: ((b: ViewBinding) -> Unit)?,
  )

  interface RuntimeItemType {
    @get:IdRes
    val typeId: Int
  }

  private var adapter: Adapter<ViewHolder>? = null
  private val differ = AsyncListDiffer(
    object : ListUpdateCallback {
      override fun onInserted(position: Int, count: Int) {
        adapter?.notifyItemRangeInserted(position, count)
      }

      override fun onRemoved(position: Int, count: Int) {
        adapter?.notifyItemRangeRemoved(position, count)
      }

      override fun onMoved(fromPosition: Int, toPosition: Int) {
        adapter?.notifyItemMoved(fromPosition, toPosition)
      }

      override fun onChanged(position: Int, count: Int, payload: Any?) {
        adapter?.notifyItemRangeChanged(position, count, payload)
      }
    },
    AsyncDifferConfig.Builder<T>(
      object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
          this@AdapterHelper.areItemsTheSame(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
          this@AdapterHelper.areContentsTheSame(oldItem, newItem)

        override fun getChangePayload(oldItem: T, newItem: T): Any? =
          this@AdapterHelper.getChangePayload(oldItem, newItem)
      },
    ).build(),
  )

  private val itemInfos = mutableListOf<ItemInfo<T>>()
  private val itemInfoByItemType = mutableMapOf<KClass<*>, ItemInfo<T>>()
  private val itemInfoByViewType = mutableMapOf<Int, ItemInfo<T>>()

  private val viewTypeGenerator = ViewTypeManager.create()

  val items: List<T>
    get() = differ.currentList

  fun <R : T, VB : ViewBinding> addItemTypeInternal(
    clazz: KClass<*>,
    viewBindingClass: KClass<VB>,
    inflateFn: (LayoutInflater, ViewGroup, Boolean) -> ViewBinding,
    bindViewHolder: (item: R, b: VB, h: ViewHolder) -> Unit,
    id: Int? = null,
    onViewCreated: ((b: VB) -> Unit)? = null,
  ) {
    val isRuntimeItemType = clazz.isRuntimeItemType
    if (!isRuntimeItemType) {
      require(itemInfoByItemType[clazz] == null) {
        "Item type $clazz has already been added."
      }
    } else if (id == null) {
      error("View type ID must be supplied when using a runtime view type.")
    }

    val itemInfo = ItemInfo(
      viewType = id ?: viewTypeGenerator.generateType(),
      viewBindingClass = viewBindingClass,
      inflateFn = inflateFn,
      bindViewHolder = { item: T, b, h ->
        @Suppress("UNCHECKED_CAST")
        bindViewHolder(item as R, b as VB, h)
      },
      onViewCreated = if (onViewCreated != null) {
        fun(viewBinding: ViewBinding) {
          @Suppress("UNCHECKED_CAST")
          onViewCreated(viewBinding as VB)
        }
      } else {
        null
      },
    )
    itemInfos.add(itemInfo)
    itemInfoByItemType[clazz] = itemInfo
    itemInfoByViewType[itemInfo.viewType] = itemInfo
  }

  fun resetItemTypes() {
    itemInfos.clear()
    itemInfoByItemType.clear()
    itemInfoByViewType.clear()
  }

  inline fun <R : T, reified VB : ViewBinding> addItemType(
    clazz: KClass<R>,
    noinline inflateFn: (LayoutInflater, ViewGroup, Boolean) -> VB,
    noinline bindViewHolder: (item: R, b: VB, h: ViewHolder) -> Unit,
  ) {
    addItemTypeInternal(clazz, VB::class, inflateFn, bindViewHolder, null)
  }

  inline fun <R : T, reified VB : ViewBinding> addItemType(
    clazz: KClass<R>,
    id: Int? = null,
    noinline inflateFn: (LayoutInflater, ViewGroup, Boolean) -> VB,
    noinline onViewCreated: ((b: VB) -> Unit),
    noinline bindViewHolder: (item: R, b: VB, h: ViewHolder) -> Unit,
  ) {
    addItemTypeInternal(clazz, VB::class, inflateFn, bindViewHolder, id, onViewCreated)
  }

  private fun getItemInfoFromPosition(position: Int): ItemInfo<T> {
    val item = items[position]
    return requireNotNull(itemInfoByItemType[item::class]) {
      "No item info for type '${item::class.java}'. Ensure this type is added."
    }
  }

  fun getItemViewType(position: Int): Int = getItemInfoFromPosition(position).viewType

  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val itemInfo = if (itemInfos.size == 1) {
      // We allow invalid view types if there is only 1 view type.
      // When only 1 view type is used, getItemViewType() is optional.
      itemInfos.first()
    } else {
      requireNotNull(itemInfoByViewType[viewType]) {
        "No item for layout id '$viewType'. Ensure this item is added. " +
          "Maybe you forgot 'override fun getItemViewType(position: Int): Int = ...'?"
      }
    }

    return ViewBindingViewHolder(
      itemInfo.inflateFn(
        LayoutInflater.from(parent.context),
        parent,
        false,
      ),
    ).also {
      itemInfo.onViewCreated?.invoke(it.binding)
    }
  }

  fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val itemInfo = getItemInfoFromPosition(position)

    val binding = holder.getBinding<ViewBinding>()
    itemInfo.bindViewHolder(items[position], binding, holder)
  }

  val itemCount: Int
    get() = differ.currentList.size

  fun setItems(newItems: List<T>, adapter: Adapter<ViewHolder>, cb: (() -> Unit)? = null) {
    this.adapter = adapter

    differ.submitList(newItems, cb)
  }
}

class ViewBindingViewHolder<T : ViewBinding>(
  val binding: T,
) : ViewHolder(binding.root)

@Suppress("unchecked_cast")
fun <T : ViewBinding> ViewHolder.getBinding() = (this as ViewBindingViewHolder<T>).binding

@Suppress("unchecked_cast")
inline fun <reified T : ViewBinding> ViewHolder.isBinding() =
  (this as? ViewBindingViewHolder<ViewBinding>)?.binding is T

private val KClass<*>.isRuntimeItemType
  get() = this.isSubclassOf(RuntimeItemType::class)
