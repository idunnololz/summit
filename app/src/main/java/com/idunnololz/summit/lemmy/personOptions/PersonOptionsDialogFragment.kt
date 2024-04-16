package com.idunnololz.summit.lemmy.personOptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.databinding.DialogFragmentPersonOptionsBinding
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss

class PersonOptionsDialogFragment :
    BaseBottomSheetDialogFragment<DialogFragmentPersonOptionsBinding>(),
    FullscreenDialogFragment {

    companion object {
        fun show(personRef: PersonRef, fragmentManager: FragmentManager) {
            PersonOptionsDialogFragment()
                .apply {
                    arguments = PersonOptionsDialogFragmentArgs(personRef)
                        .toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "PersonOptionsDialogFragment")
        }
    }

    private val args: PersonOptionsDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentPersonOptionsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
