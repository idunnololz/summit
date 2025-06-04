package com.idunnololz.summit.lemmy.inbox.inbox

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.databinding.DialogFragmentReasonBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.setSizeDynamically
import kotlinx.parcelize.Parcelize

class ReasonDialogFragment :
  BaseDialogFragment<DialogFragmentReasonBinding>() {

  companion object {
    fun show(fragmentManager: FragmentManager, title: String, positiveButton: String) =
      ReasonDialogFragment()
        .apply {
          arguments = ReasonDialogFragmentArgs(
            title = title,
            positiveButton = positiveButton,
          ).toBundle()
        }
        .show(fragmentManager, "ReasonDialogFragment")

    const val REQUEST_KEY = "ReasonDialogFragment_req_key"
    const val RESULT_KEY = "result"
  }

  private val args: ReasonDialogFragmentArgs by navArgs()

  @Parcelize
  data class Result(
    val isOk: Boolean,
    val reason: String?,
  ) : Parcelable

  override fun onStart() {
    super.onStart()
    setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentReasonBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      title.text = args.title
      positiveButton.text = args.positiveButton

      positiveButton.setOnClickListener {
        setFragmentResult(REQUEST_KEY, Bundle().apply {
          putParcelable(RESULT_KEY, Result(
            isOk = true,
            reason = reasonEditText.text.toString()
          ))
        })
        dismiss()
      }
      cancel.setOnClickListener {
        setFragmentResult(REQUEST_KEY, Bundle().apply {
          putParcelable(RESULT_KEY, Result(
            isOk = false,
            reason = null
          ))
        })
        dismiss()
      }
    }
  }
}