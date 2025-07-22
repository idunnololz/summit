package com.idunnololz.summit.uploads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentUploadsBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UploadsFragment : BaseFragment<FragmentUploadsBinding>() {

  private val viewModel: UploadsViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentUploadsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      setupToolbar(toolbar, getString(R.string.uploads))

    }
  }
}
