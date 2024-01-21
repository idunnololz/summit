package com.idunnololz.summit.receiveFIle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentReceiveFileBinding
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.saveForLater.SaveForLaterDialogFragment
import com.idunnololz.summit.saveForLater.SaveForLaterDialogFragmentArgs
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.setupBottomSheetAndShow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReceiveFileDialogFragment : BaseDialogFragment<DialogFragmentReceiveFileBinding>() {

    private val args by navArgs<ReceiveFileDialogFragmentArgs>()

    @Inject
    lateinit var accountManager: AccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                window.setBackgroundDrawable(null)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setWindowAnimations(R.style.BottomSheetAnimations)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentReceiveFileBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheetAndShow(
            bottomSheet = binding.bottomSheet,
            bottomSheetContainerInner = binding.bottomSheetContainerInner,
            overlay = binding.overlay,
            onClose = {
                dismiss()
            },
        )

        requireMainActivity().apply {
            doOnInsetChanged(viewLifecycleOwner) { insets ->
                binding.bottomSheet.updatePadding(bottom = insets.bottom)
                binding.bottomSheetContainerInner.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = insets.top
                }
            }
        }

        with(binding) {
            preview.load(args.fileUri)

            createPost.setOnClickListener {
                val account = accountManager.currentAccount.value

                if (account == null) {
                    AlertDialogFragment.Builder()
                        .setMessage(R.string.you_must_sign_in_to_create_a_post)
                        .createAndShow(childFragmentManager, "asdf")
                    return@setOnClickListener
                }

                CreateOrEditPostFragment()
                    .apply {
                        arguments = CreateOrEditPostFragmentArgs(
                            instance = account.instance,
                            communityName = null,
                            post = null,
                            crosspost = null,
                            extraStream = args.fileUri,
                        ).toBundle()
                    }
                    .show(parentFragmentManager, "CreateOrEditPostFragment")
                dismiss()
            }
            saveForLater.setOnClickListener {
                SaveForLaterDialogFragment()
                    .apply {
                        arguments = SaveForLaterDialogFragmentArgs(
                            args.fileUri,
                        ).toBundle()
                    }
                    .show(parentFragmentManager, "SaveForLaterDialogFragment")
                dismiss()
            }
            cancel.setOnClickListener {
                dismiss()
            }
        }
    }
}
