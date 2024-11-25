package com.idunnololz.summit.settings.webSettings.changePassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentChangePasswordBinding
import com.idunnololz.summit.util.BaseDialogFragment

class ChangePasswordDialogFragment : BaseDialogFragment<DialogFragmentChangePasswordBinding>() {

    companion object {
        fun show(fragmentManager: FragmentManager) {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentChangePasswordBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}