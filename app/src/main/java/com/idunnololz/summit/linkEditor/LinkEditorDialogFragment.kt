package com.idunnololz.summit.linkEditor

import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentLinkEditorBinding
import com.idunnololz.summit.databinding.LinkPartBinding
import com.idunnololz.summit.databinding.LinkPartParamBinding
import com.idunnololz.summit.feedback.PostFeedbackDialogFragment
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.VerticalSpaceItemDecoration
import com.idunnololz.summit.util.ext.setTextIfChanged
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupToolbar
import kotlinx.coroutines.launch

// clean up https://lemmy.zip/api/v3/image_proxy?url=https%3A%2F%2Feurope.pub%2Fapi%2Fv3%2Fimage_proxy%3Furl%3Dhttps%253A%252F%252Flemmy.ml%252Fpictrs%252Fimage%252F288a95d3-b040-475c-9b8c-6fe4fe14fcac.png
class LinkEditorDialogFragment : BaseDialogFragment<DialogFragmentLinkEditorBinding>(),
  FullscreenDialogFragment {

  companion object {
    fun show(supportFragmentManager: FragmentManager, url: String) {
      LinkEditorDialogFragment()
        .apply {
          arguments = LinkEditorDialogFragmentArgs(url).toBundle()
        }
        .show(supportFragmentManager, "LinkEditorDialogFragment")
    }
  }

  private val args by navArgs<LinkEditorDialogFragmentArgs>()
  private val viewModel by viewModels<LinkEditorViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentLinkEditorBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
        insetViewAutomaticallyByPadding(
          viewLifecycleOwner,
          binding.content,
          applyTopInset = false,
        )
      }

      val adapter = LinkDataAdapter(
        onPartsChanged = {
          viewModel.onPartsChanged(it)
        }
      )
      recyclerView.setHasFixedSize(true)
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.adapter = adapter
      recyclerView.itemAnimator = null
      recyclerView.addItemDecoration(
        VerticalSpaceItemDecoration(
          verticalSpaceHeight = Utils.convertDpToPixel(1f).toInt(),
          hasStartAndEndSpace = false
        )
      )
      setupToolbar(toolbar, getString(R.string.link_editor))

      link.setEndIconOnClickListener {
        Utils.copyToClipboard(context, viewModel.currentUrl.value)
      }

      viewLifecycleOwner.lifecycleScope.launch {
        viewModel.currentUrl.collect {
          linkEditText.setTextIfChanged(it) {
            viewModel.currentUrl.value = it
          }
        }
      }
      viewModel.data.observe(viewLifecycleOwner) {
        when(it) {
          is StatefulData.Error -> loadingView.showDefaultErrorMessageFor(it.error)
          is StatefulData.Loading -> loadingView.showProgressBar()
          is StatefulData.NotStarted -> loadingView.hideAll()
          is StatefulData.Success -> {
            loadingView.hideAll()

            adapter.data = it.data
          }
        }
      }
    }

    if (viewModel.currentUrl.value.isBlank()) {
      viewModel.currentUrl.value = args.url
    }
  }

  private class LinkDataAdapter(
    val onPartsChanged: (List<LinkPart>) -> Unit,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val adapterHelper = AdapterHelper<LinkPart>(
      { old, new ->
        isSamePart(old, new)
      }
    ).apply {
      addItemType(LinkPart.Authority::class, LinkPartBinding::inflate) { item, b, h ->
        b.label.setText("authority")
        b.label.setOnClickListener {
          Utils.copyToClipboard(b.label.context, item.value)
        }
        b.editText.setTextIfChanged(item.value) {
          updatePart(item.copy(value = it))
        }
      }
      addItemType(LinkPart.Fragment::class, LinkPartBinding::inflate) { item, b, h ->
        b.label.setText("fragment")
        b.label.setOnClickListener {
          Utils.copyToClipboard(b.label.context, item.value)
        }
        b.editText.setTextIfChanged(item.value) {
          updatePart(item.copy(value = it))
        }
      }
      addItemType(LinkPart.Path::class, LinkPartBinding::inflate) { item, b, h ->
        b.label.setText("path")
        b.label.setOnClickListener {
          Utils.copyToClipboard(b.label.context, item.value)
        }
        b.editText.setTextIfChanged(item.value) {
          updatePart(item.copy(value = it))
        }
      }
      addItemType(LinkPart.QueryParam::class, LinkPartParamBinding::inflate) { item, b, h ->
        b.label.setText("query param")
        b.label.setOnClickListener {
          Utils.copyToClipboard(b.label.context, item.value)
        }
        b.key.setTextIfChanged(item.key) {
          updatePart(item.copy(key = it))
        }
        b.editText.setTextIfChanged(item.value) {
          updatePart(item.copy(value = it))
        }
      }
      addItemType(
        clazz = LinkPart.Scheme::class,
        inflateFn = LinkPartBinding::inflate,
      ) { item, b, h ->
        b.label.setText("scheme")
        b.label.setOnClickListener {
          Utils.copyToClipboard(b.label.context, item.value)
        }

        b.editText.setTextIfChanged(item.value) {
          updatePart(item.copy(value = it))
        }
      }
    }

    var data: LinkEditorModel? = null
      set(value) {
        field = value

        refreshItems()
      }

    override fun getItemViewType(position: Int): Int =
      adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int,
    ): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(
      holder: RecyclerView.ViewHolder,
      position: Int,
    ) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int =
      adapterHelper.itemCount

    private fun refreshItems() {
      val data = data ?: return

      adapterHelper.setItems(data.linkParts, this)
    }

    private fun updatePart(part: LinkPart) {
      val data = data ?: return

      val index = data.linkParts.indexOfFirst { isSamePart(it, part) }
      if (index >= 0) {
        val newList = data.linkParts.toMutableList()
        newList[index] = part
        onPartsChanged(newList)
      }
    }

    private fun isSamePart(old: LinkPart, new: LinkPart) =
      old::class == new::class && when (old) {
        is LinkPart.Authority -> true
        is LinkPart.Fragment -> true
        is LinkPart.Path -> true
        is LinkPart.QueryParam ->
          old.internalKey == (new as LinkPart.QueryParam).internalKey
        is LinkPart.Scheme -> true
      }
  }
}