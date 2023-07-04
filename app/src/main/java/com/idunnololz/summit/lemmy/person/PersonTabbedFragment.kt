package com.idunnololz.summit.lemmy.person

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.databinding.FragmentPersonBinding
import com.idunnololz.summit.lemmy.inbox.InboxFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ViewPagerAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersonTabbedFragment : BaseFragment<FragmentPersonBinding>() {

    private val args by navArgs<PersonTabbedFragmentArgs>()

    private val viewModel: PersonTabbedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentPersonBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {

            setupForFragment<PersonTabbedFragment>()

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = ""

            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.appBar)
        }

        viewModel.fetchPersonIfNotDone(args.personRef)

        with(binding) {
            viewModel.personData.observe(viewLifecycleOwner) {
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

                        setup()
                    }
                }
            }
        }
    }

    private fun setup() {
        if (!isBindingAvailable()) return

        val data = viewModel.personData.valueOrNull ?: return

        requireMainActivity().apply {
            supportActionBar?.title = data.personView.person.name
        }

    }
}