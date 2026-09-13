package com.idunnololz.summit.drafts.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.databinding.FragmentPostAndCommentTemplatesBinding
import com.idunnololz.summit.drafts.DraftsDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss

class PostAndCommentTemplatesFragment  :
  BaseFragment<FragmentPostAndCommentTemplatesBinding>(),
  FullscreenDialogFragment {

  companion object {
    const val REQUEST_KEY = "DraftsDialogFragment_req_key"
    const val REQUEST_KEY_RESULT = "result"

    fun show(fragmentManager: FragmentManager) {
      DraftsDialogFragment().apply {
      }.showAllowingStateLoss(fragmentManager, "PostsAndCommentsTemplatesFragment")
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentPostAndCommentTemplatesBinding.inflate(inflater, container, false))

    return binding.root
  }


  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {

    }
  }

}