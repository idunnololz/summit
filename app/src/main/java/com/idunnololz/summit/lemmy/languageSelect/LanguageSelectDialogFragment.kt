package com.idunnololz.summit.lemmy.languageSelect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.Language
import com.idunnololz.summit.api.dto.LanguageId
import com.idunnololz.summit.databinding.DialogFragmentLanguageSelectBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss

class LanguageSelectDialogFragment : BaseDialogFragment<DialogFragmentLanguageSelectBinding>() {

    companion object {
        const val REQUEST_KEY = "LanguageSelectDialogFragment_req"

        fun show(
            languages: List<Language>,
            selectedLanguages: List<LanguageId>,
            fragmentManager: FragmentManager,
        ) {
            LanguageSelectDialogFragment()
                .apply {
                    arguments = LanguageSelectDialogFragmentArgs(
                        languages.toTypedArray(),
                        selectedLanguages.toIntArray(),
                    ).toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "aaa")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentLanguageSelectBinding.inflate(inflater, container, false))

        return binding.root
    }
}