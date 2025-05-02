package com.idunnololz.summit.lemmy.userTags

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil3.load
import com.idunnololz.summit.MainDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.databinding.DialogFragmentAddOrEditUserTagBinding
import com.idunnololz.summit.databinding.GenericShimmerPlaceholderBinding
import com.idunnololz.summit.databinding.RecentTagItemEmptyBinding
import com.idunnololz.summit.databinding.RecentTagItemUserTagBinding
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.personPicker.PersonPickerDialogFragment
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.colorPicker.ColorPickerDialog
import com.idunnololz.summit.util.colorPicker.OnColorPickedListener
import com.idunnololz.summit.util.colorPicker.utils.ColorPicker
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.isLoading
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.shimmer.newShimmerDrawable
import com.idunnololz.summit.util.valueOrNull
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
      width = LayoutParams.MATCH_PARENT,
      height = LayoutParams.WRAP_CONTENT,
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
        },
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
      viewTags.setOnClickListener {
        requireMainActivity().openAllUserTagsFragment()
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

      recentTagsRecyclerView.layoutManager = LinearLayoutManager(
        context, LinearLayoutManager.HORIZONTAL, false)
      recentTagsRecyclerView.setHasFixedSize(true)
      val adapter = RecentUserTagsAdapter(
        context,
        onClickUserTag = {
          viewModel.tag = it.config.tagName
          viewModel.fillColor = it.config.fillColor
          viewModel.strokeColor = it.config.borderColor
        }
      )
      recentTagsRecyclerView.adapter = adapter

      viewModel.recentTags.observe(viewLifecycleOwner) {
        adapter.setData(it)
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
      if (model.personNameError != null) {
        personInputLayout.error = getString(model.personNameError)
      } else {
        personInputLayout.isErrorEnabled = false
      }

      if (!tagEditText.isFocused) {
        tagEditText.setText(model.tag)
      }
      if (model.tagError != null) {
        tagInputLayout.error = getString(model.tagError)
      } else {
        tagInputLayout.isErrorEnabled = false
      }

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

  private class RecentUserTagsAdapter(
    private val context: Context,
    private val onClickUserTag: (UserTag) -> Unit,
  ) : Adapter<ViewHolder>() {

    private sealed interface Item {
      data class PlaceholderItem(val id: Int) : Item
      data class UserTagItem(val userTag: UserTag) : Item

      data object EmptyItem : Item
    }

    private var data: StatefulData<List<UserTag>>? = null

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class && when (old) {
          is Item.PlaceholderItem ->
            old.id == (new as Item.PlaceholderItem).id
          is Item.UserTagItem ->
            old.userTag.id == (new as Item.UserTagItem).userTag.id
          Item.EmptyItem -> true
        }
      }
    ).apply {
      addItemType(
        Item.PlaceholderItem::class,
        GenericShimmerPlaceholderBinding::inflate,
      ) { item, b, h ->
        b.root.updateLayoutParams<LayoutParams> {
          width = Utils.convertDpToPixel(96f).toInt()
          height = Utils.convertDpToPixel(48f).toInt()
        }
        b.root.load(newShimmerDrawable(context, 0.5f))
      }
      addItemType(
        Item.UserTagItem::class,
        RecentTagItemUserTagBinding::inflate,
      ) { item, b, h ->
        b.chip.text = item.userTag.config.tagName
        val borderColor = ColorStateList.valueOf(item.userTag.config.borderColor)
        b.chip.chipBackgroundColor = ColorStateList.valueOf(item.userTag.config.fillColor)
        b.chip.chipStrokeColor = borderColor
        b.chip.setTextColor(borderColor)
        b.chip.setOnClickListener {
          onClickUserTag(item.userTag)
        }
      }
      addItemType(
        Item.EmptyItem::class,
        RecentTagItemEmptyBinding::inflate,
      ) { item, b, h ->
      }
    }

    override fun getItemViewType(position: Int): Int =
      adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int =
      adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    fun setData(data: StatefulData<List<UserTag>>) {
      this.data = data

      refresh()
    }

    private fun refresh() {
      val data = data ?: return
      val newItems = mutableListOf<Item>()

      if (data.isLoading) {
        newItems += Item.PlaceholderItem(0)
        newItems += Item.PlaceholderItem(1)
        newItems += Item.PlaceholderItem(2)
      } else {
        val recentTags = data.valueOrNull
        if (recentTags?.isEmpty() == true) {
          newItems += Item.EmptyItem
        } else {
          recentTags?.mapTo(newItems) {
            Item.UserTagItem(it)
          }
        }
      }

      adapterHelper.setItems(newItems, this)
    }
  }
}
