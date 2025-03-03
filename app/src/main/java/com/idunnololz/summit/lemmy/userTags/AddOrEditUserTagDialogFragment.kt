package com.idunnololz.summit.lemmy.userTags

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.databinding.DialogFragmentAddOrEditUserTagBinding
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.personPicker.PersonPickerDialogFragment
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.colorPicker.ColorPickerDialog
import com.idunnololz.summit.util.colorPicker.OnColorPickedListener
import com.idunnololz.summit.util.colorPicker.utils.ColorPicker
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.getParcelableCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddOrEditUserTagDialogFragment : BaseDialogFragment<DialogFragmentAddOrEditUserTagBinding>() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            person: Person? = null,
            userTag: UserTag? = null,
            personRef: PersonRef? = null,
        ) {
            AddOrEditUserTagDialogFragment()
                .apply {
                    arguments = AddOrEditUserTagDialogFragmentArgs(
                        person = person,
                        userTag = userTag,
                        personRef = personRef,
                    ).toBundle()
                }
                .show(fragmentManager, "AddOrEditUserTagDialogFragment")
        }
    }

    private val args: AddOrEditUserTagDialogFragmentArgs by navArgs()

    private val viewModel: AddOrEditUserTagViewModel by viewModels()

    @Inject
    lateinit var globalStateStorage: GlobalStateStorage

    override fun onStart() {
        super.onStart()

        setSizeDynamically(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentAddOrEditUserTagBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        if (savedInstanceState == null) {
            val person = args.person
            val userTag = args.userTag
            val personRef = args.personRef
            if (person != null) {
                viewModel.personName = person.fullName
            }
            if (userTag != null) {
                viewModel.personName = userTag.personName
            }
            if (personRef != null) {
                viewModel.personName = personRef.fullName
            }

            viewModel.fillColor =
                context.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
            viewModel.strokeColor =
                context.getColorFromAttribute(com.google.android.material.R.attr.colorOnPrimary)
        }

        childFragmentManager.setFragmentResultListener(
            PersonPickerDialogFragment.REQUEST_KEY,
            this,
        ) { key, bundle ->
            val result = bundle.getParcelableCompat<PersonPickerDialogFragment.Result>(
                PersonPickerDialogFragment.REQUEST_KEY_RESULT,
            )
            if (result != null) {
                viewModel.personName = result.personRef.fullName
            }
        }

        with(binding) {
            toolbar.setTitle(R.string.add_user_tag)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(android.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            personEditText.setOnFocusChangeListener { view, b ->
                showPersonPicker()
            }
            tagEditText.addTextChangedListener(
                onTextChanged = { text, start, before, count ->
                    viewModel.tag = text?.toString() ?: ""
                }
            )

            changeFillColorButton.setOnClickListener {
                showColorPicker(viewModel.fillColor) {
                    viewModel.fillColor = it
                }
            }
            changeStrokeColorButton.setOnClickListener {
                showColorPicker(viewModel.strokeColor) {
                    viewModel.strokeColor = it
                }
            }

            positiveButton.setOnClickListener {
                // Remember the values before we set them as setting them will revert their values
                val personName = requireNotNull(binding.personEditText.text?.toString())
                val tag = requireNotNull(binding.tagEditText.text?.toString())

                viewModel.personName = personName
                viewModel.tag = tag

                viewModel.addTag()
            }
            negativeButton.setOnClickListener {
                dismiss()
            }
            neutralButton.setOnClickListener {
                viewModel.deleteUserTag()
                dismiss()
            }

            viewModel.model.observe(viewLifecycleOwner) {
                updateRender()
            }
        }
    }

    override fun onPause() {
        viewModel.personName = requireNotNull(binding.personEditText.text?.toString())
        viewModel.tag = requireNotNull(binding.tagEditText.text?.toString())

        super.onPause()
    }

    private fun showPersonPicker() {
        val personEditText = binding.personEditText

        if (personEditText.hasFocus()) {
            val prefill = personEditText.text?.split("@")?.firstOrNull().toString()

            personEditText.clearFocus()
            PersonPickerDialogFragment.show(childFragmentManager, prefill)
        }
    }

    private fun updateRender() {
        val model = viewModel.model.value ?: return

        if (model.isSubmitted) {
            dismiss()
            return
        }

        with(binding) {
            personEditText.setText(model.personName)
            personInputLayout.error = model.personNameError

            if (!tagEditText.isFocused) {
                tagEditText.setText(model.tag)
            }
            tagInputLayout.error = model.tagError

            tagFillColorInner.background = ColorDrawable(model.fillColor)
            tagStrokeColorInner.background = ColorDrawable(model.strokeColor)
            neutralButton.visibility = if (model.showDeleteButton) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun showColorPicker(initialColor: Int, onColorPicked: (Int) -> Unit) {
        val context = requireContext()

        ColorPickerDialog(
            context = context,
            title = context.getString(R.string.tag_fill_color),
            color = initialColor,
            globalStateStorage = globalStateStorage,
        )
            .withAlphaEnabled(true)
            .withListener(object : OnColorPickedListener {
                override fun onColorPicked(pickerView: ColorPicker?, color: Int) {
                    onColorPicked(color)
                }
            })
            .show()
    }
}
