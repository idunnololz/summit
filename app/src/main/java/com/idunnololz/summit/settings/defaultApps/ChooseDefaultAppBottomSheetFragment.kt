package com.idunnololz.summit.settings.defaultApps

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentChooseDefaultAppBinding
import com.idunnololz.summit.databinding.ItemAppChoiceBinding
import com.idunnololz.summit.databinding.ItemAppChoiceClearBinding
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import kotlinx.parcelize.Parcelize

class ChooseDefaultAppBottomSheetFragment :
    BaseBottomSheetDialogFragment<FragmentChooseDefaultAppBinding>(),
    FullscreenDialogFragment {

    companion object {

        const val RESULT_KEY = "ChooseDefaultAppBottomSheetFragment.result"
        const val REQUEST_KEY = "ChooseDefaultAppBottomSheetFragment.request"

        fun show(fragmentManager: FragmentManager, intent: Intent) =
            ChooseDefaultAppBottomSheetFragment()
                .apply {
                    arguments = ChooseDefaultAppBottomSheetFragmentArgs(
                        intent
                    ).toBundle()
                }
                .show(fragmentManager, "ChooseDefaultAppBottomSheetFragment")
    }

    @Parcelize
    class Result(
        val selectedApp: ApplicationInfo?,
        val clear: Boolean,
    ): Parcelable

    private val args: ChooseDefaultAppBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentChooseDefaultAppBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val intent = args.intent
        val pm = context.packageManager
        val options: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            } else {
                pm.queryIntentActivities(intent, 0)
            }.mapNotNull { it }

        with(binding) {
            recyclerView.adapter = ResolveInfoAdapter(
                pm,
                options,
                {
                    setFragmentResult(
                        REQUEST_KEY,
                        Bundle().apply {
                            putParcelable(RESULT_KEY, Result(
                                selectedApp = it,
                                clear = it == null,
                            ))
                        }
                    )
                    dismiss()
                },
            )
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(false)
        }
    }

    private class ResolveInfoAdapter(
        private val packageManager: PackageManager,
        private val options: List<ResolveInfo>,
        private val onIntentClick: (ApplicationInfo?) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data class OptionItem(
                val resolveInfo: ResolveInfo
            ): Item

            data object ClearItem : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    Item.ClearItem -> true
                    is Item.OptionItem ->
                        old.resolveInfo.resolvePackageName ==
                            (new as Item.OptionItem).resolveInfo.resolvePackageName
                }
            }
        ).apply {
            addItemType(Item.OptionItem::class, ItemAppChoiceBinding::inflate) { item, b, h ->
                val appInfo = item.resolveInfo.activityInfo.applicationInfo
                b.icon.load(packageManager.getApplicationIcon(appInfo))
                b.title.text = packageManager.getApplicationLabel(appInfo)

                b.root.setOnClickListener {
                    onIntentClick(appInfo)
                }
            }
            addItemType(Item.ClearItem::class, ItemAppChoiceClearBinding::inflate) { item, b, h ->
                b.root.setOnClickListener {
                    onIntentClick(null)
                }
            }
        }

        init {
            val newItems = mutableListOf<Item>()
            options.mapTo(newItems) {
                Item.OptionItem(it)
            }
            newItems += Item.ClearItem
            adapterHelper.setItems(newItems, this)
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)
    }
}