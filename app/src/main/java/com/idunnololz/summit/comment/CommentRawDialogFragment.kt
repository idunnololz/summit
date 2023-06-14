package com.idunnololz.summit.comment

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.*
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentCommentRawBinding
import com.idunnololz.summit.util.BaseDialogFragment
import java.util.*
import kotlin.collections.ArrayList

class CommentRawDialogFragment : BaseDialogFragment<DialogFragmentCommentRawBinding>() {

    private val args: CommentRawDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)

        builder.setMessage(R.string.raw_comment)

        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.dialog_fragment_comment_raw, null)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerView)

        builder.setView(rootView)

        builder.setPositiveButton(android.R.string.ok) { _, _ -> }

        recyclerView.adapter = JsonAdapter(context).apply {
            setJsonNode(generateIntermediateTree(JsonParser().parse(args.commentItemStr)))
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)

        return builder.create().also { dialog ->
        }
    }

    private fun generateIntermediateTree(elem: JsonElement): JsonNode =
        when {
            elem.isJsonObject -> {
                val jsonObj = elem.asJsonObject
                JsonNode.JsonNodeObj(jsonObj, jsonObj.entrySet().associate {
                    it.key to generateIntermediateTree(it.value)
                })
            }
            elem.isJsonArray -> {
                val jsonArr = elem.asJsonArray
                JsonNode.JsonNodeArray(jsonArr, jsonArr.map { generateIntermediateTree(it) })
            }
            elem.isJsonPrimitive -> {
                JsonNode.JsonNodePrimitive(elem.asJsonPrimitive)
            }
            elem.isJsonNull -> JsonNode.JsonNodeNull
            else -> JsonNode.JsonNodeNull
        }

    private sealed class JsonNode(
        val isExpanded: Boolean = true
    ) {
        data class JsonNodePrimitive(
            val jsonPrimitive: JsonPrimitive
        ) : JsonNode()

        data class JsonNodeArray(
            val jsonArray: JsonArray,
            val children: List<JsonNode>
        ) : JsonNode()

        data class JsonNodeObj(
            val jsonObject: JsonObject,
            val children: Map<String, JsonNode>
        ) : JsonNode()

        object JsonNodeNull : JsonNode()
    }

    private data class ToProcessObj(
        val key: String,
        val value: JsonNode,
        val currentDepth: Int,
        val isEnd: Boolean = false
    )

    private class Item(
        val key: String,
        val depth: Int,
        val isExpanded: Boolean,
        val raw: JsonNode,
        val value: JsonPrimitive? = null,
        val isObj: Boolean = false,
        val isArray: Boolean = false,
        val isNull: Boolean = false,
        val isEnd: Boolean = false,
        val isEmpty: Boolean = false
    )

    private class ItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textView: TextView = v.findViewById(R.id.text)
    }

    private class JsonAdapter(
        private val context: Context,
        private val indentSize: Int = context.resources.getDimensionPixelOffset(R.dimen.padding_half)
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        private var raw: JsonNode = JsonNode.JsonNodeNull
        private var items: List<Item> = listOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = inflater.inflate(R.layout.json_node_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val h = holder as ItemViewHolder
            val item = items[position]

            h.textView.layoutParams =
                (h.textView.layoutParams as ConstraintLayout.LayoutParams).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        marginStart = indentSize * item.depth
                    } else {
                        leftMargin = indentSize * item.depth
                    }
                }
            h.textView.text = buildString {
                if (item.key.isNotEmpty() && !item.isEnd) {
                    append(item.key)
                    append(": ")
                }
                when {
                    item.isArray -> {
                        if (item.isEnd) {
                            append("]")
                        } else {
                            append("[")
                        }
                        if (item.isEmpty) {
                            append(" ]")
                        }
                    }
                    item.isObj -> {
                        if (item.isEnd) {
                            append("}")
                        } else {
                            append("{")
                        }
                        if (item.isEmpty) {
                            append(" }")
                        }
                    }
                    else -> {
                        append(item.value)
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun setJsonNode(node: JsonNode) {
            raw = node

            refresh()
        }

        private fun refresh() {
            val newItems = generateItems(raw)
            val oldItems = items

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition].raw === newItems[newItemPosition].raw
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]
                    return oldItem.isExpanded == newItem.isExpanded && areContentsTheSame(
                        oldItemPosition,
                        newItemPosition
                    )
                }

            })
            this.items = newItems
            diff.dispatchUpdatesTo(this)
        }

        private fun generateItems(node: JsonNode): ArrayList<Item> {
            val result = arrayListOf<Item>()
            val toProcess = Stack<ToProcessObj>()
            toProcess.push(ToProcessObj("", node, 0))

            while (toProcess.isNotEmpty()) {
                val toProcessObj = toProcess.pop()
                val (key, elem, currentDepth) = toProcessObj

                when (elem) {
                    is JsonNode.JsonNodePrimitive -> {
                        result.add(
                            Item(
                                key,
                                depth = currentDepth,
                                isExpanded = elem.isExpanded,
                                raw = elem,
                                value = elem.jsonPrimitive
                            )
                        )
                    }
                    is JsonNode.JsonNodeArray -> {
                        if (toProcessObj.isEnd) {
                            result.add(
                                Item(
                                    key,
                                    depth = currentDepth,
                                    isExpanded = elem.isExpanded,
                                    raw = elem,
                                    isArray = true,
                                    isEnd = true
                                )
                            )
                        } else {
                            val isEmpty = elem.children.isEmpty()
                            result.add(
                                Item(
                                    key,
                                    depth = currentDepth,
                                    isExpanded = elem.isExpanded,
                                    raw = elem,
                                    isArray = true,
                                    isEmpty = isEmpty
                                )
                            )

                            if (!isEmpty) {
                                toProcess.push(ToProcessObj("", elem, currentDepth, isEnd = true))
                            }
                            if (elem.isExpanded) {
                                elem.children.reversed().forEach {
                                    toProcess.push(ToProcessObj("", it, currentDepth + 1))
                                }
                            }
                        }
                    }
                    is JsonNode.JsonNodeObj -> {
                        if (toProcessObj.isEnd) {
                            result.add(
                                Item(
                                    key,
                                    depth = currentDepth,
                                    isExpanded = elem.isExpanded,
                                    raw = elem,
                                    isObj = true,
                                    isEnd = true
                                )
                            )
                        } else {
                            val isEmpty = elem.children.isEmpty()
                            result.add(
                                Item(
                                    key,
                                    depth = currentDepth,
                                    isExpanded = elem.isExpanded,
                                    raw = elem,
                                    isObj = true,
                                    isEmpty = isEmpty
                                )
                            )

                            if (!isEmpty) {
                                toProcess.push(ToProcessObj("", elem, currentDepth, isEnd = true))
                            }
                            if (elem.isExpanded) {
                                elem.children.entries.reversed().forEach {
                                    toProcess.push(ToProcessObj(it.key, it.value, currentDepth + 1))
                                }
                            }
                        }
                    }
                    JsonNode.JsonNodeNull -> {
                        result.add(
                            Item(
                                key,
                                isExpanded = elem.isExpanded,
                                raw = elem,
                                depth = currentDepth,
                                isNull = true
                            )
                        )
                    }
                }
            }

            return result
        }
    }

}