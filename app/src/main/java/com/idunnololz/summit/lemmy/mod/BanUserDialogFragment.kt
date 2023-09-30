package com.idunnololz.summit.lemmy.mod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentBanUserBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BanUserDialogFragment : BaseDialogFragment<DialogFragmentBanUserBinding>() {

    private val args by navArgs<BanUserDialogFragmentArgs>()

    private val actionsViewModel: MoreActionsViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            val window = checkNotNull(dialog.window)
            window.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentBanUserBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            actionsViewModel.banUserResult.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        binding.progressBar.visibility = View.GONE

                        ErrorDialogFragment.show(
                            getString(R.string.unable_to_ban_user),
                            it.error,
                            childFragmentManager,
                        )
                        disableViews(false)
                    }
                    is StatefulData.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        disableViews(true)
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        binding.progressBar.visibility = View.GONE
                        disableViews(false)
                        dismiss()
                    }
                }
            }

            binding.cancel.setOnClickListener {
                dismiss()
            }
            binding.banUser.setOnClickListener {
                actionsViewModel.banUser(
                    args.communityId,
                    args.personId,
                    true,
                    removeContent.isChecked,
                    reasonEditText.text.toString().let {
                        it.ifBlank {
                            null
                        }
                    },
                    numberOfDaysEditText.text.toString().toIntOrNull(),
                )
            }
        }
    }

    fun disableViews(disable: Boolean) {
        if (!isBindingAvailable()) {
            return
        }

        with(binding) {
            reason.isEnabled = !disable
            numberOfDays.isEnabled = !disable
            removeContent.isEnabled = !disable
            cancel.isEnabled = !disable
            banUser.isEnabled = !disable
        }
    }
}
