package com.idunnololz.summit.emptyScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.databinding.FragmentEmptyScreenBinding
import com.idunnololz.summit.util.BaseFragment

class EmptyScreenFragment : BaseFragment<FragmentEmptyScreenBinding>() {

    companion object {

        private const val ARG_TEXT = "ARG_TEXT"

        fun newInstance(text: String) =
            EmptyScreenFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentEmptyScreenBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val text = requireArguments().getString(ARG_TEXT)

        with(binding) {
            textView.text = text
        }
    }
}
