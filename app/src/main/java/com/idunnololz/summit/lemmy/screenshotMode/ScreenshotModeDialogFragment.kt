package com.idunnololz.summit.lemmy.screenshotMode

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.divider.MaterialDivider
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentScreenshotModeBinding
import com.idunnololz.summit.databinding.ScreenshotBottomBarBinding
import com.idunnololz.summit.databinding.ScreenshotStageBinding
import com.idunnololz.summit.lemmy.post.ModernThreadLinesDecoration
import com.idunnololz.summit.lemmy.post.PostAdapter
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ScreenshotWatermarkId
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.MimeTypes
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.shareUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class ScreenshotModeDialogFragment :
    BaseDialogFragment<DialogFragmentScreenshotModeBinding>(),
    FullscreenDialogFragment {

    companion object {
        const val REQUEST_KEY = "ScreenshotModeDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        private const val TAG = "ScreenshotModeDialog"

        fun show(fragmentManager: FragmentManager) {
            ScreenshotModeDialogFragment()
                .showAllowingStateLoss(fragmentManager, "ScreenshotModeDialogFragment")
        }
    }

    @Parcelize
    data class Result(
        val uri: Uri,
        val mimeType: String,
    ) : Parcelable

    private val viewModel: ScreenshotModeViewModel by viewModels()

    @Inject
    lateinit var preferences: Preferences

    private var uriToSave: Uri? = null
    private val createPngDocumentLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(MimeTypes.PNG),
        ) { result ->
            onCreateDocumentResult(result)
        }
    private val createGifDocumentLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(MimeTypes.GIF),
        ) { result ->
            onCreateDocumentResult(result)
        }
    private val createMp4DocumentLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(MimeTypes.MP4),
        ) { result ->
            onCreateDocumentResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentScreenshotModeBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parent = (parentFragment as? PostFragment)
        val context = requireContext()

        if (parent == null) {
            dismiss()
            return
        }

        val adapter = parent.getAdapter()

        if (adapter == null) {
            dismiss()
            return
        }

        requireMainActivity().apply {
            insetViewAutomaticallyByPadding(
                viewLifecycleOwner,
                binding.container,
                Utils.convertDpToPixel(100f).toInt(),
            )
        }

        generateScreenshot(adapter)

        binding.fab.setOnClickListener {
            val infographicsView = binding.zoomLayout.children.firstOrNull()
            if (infographicsView != null) {
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                viewModel.generateImageToSave(infographicsView, "post_screenshot_$ts")
            }
        }
        binding.zoomLayout.isClickable = false

        viewModel.screenshotConfig.observe(viewLifecycleOwner) { config ->

            val nextPostViewType = config.postViewType

            binding.bottomAppBar.menu.findItem(R.id.toggle_post_view)?.apply {
                when (nextPostViewType) {
                    ScreenshotModeViewModel.PostViewType.Full -> {
                        setIcon(R.drawable.baseline_view_list_24)
                        setTitle(R.string.full_post)
                    }
                    ScreenshotModeViewModel.PostViewType.ImageOnly -> {
                        setIcon(R.drawable.baseline_image_24)
                        setTitle(R.string.post_image_only)
                    }
                    ScreenshotModeViewModel.PostViewType.TextOnly -> {
                        setIcon(R.drawable.baseline_text_fields_24)
                        setTitle(R.string.post_text_only)
                    }
                    ScreenshotModeViewModel.PostViewType.TitleOnly -> {
                        setIcon(R.drawable.baseline_title_24)
                        setTitle(R.string.post_title_only)
                    }
                    ScreenshotModeViewModel.PostViewType.TitleAndImageOnly -> {
                        setIcon(R.drawable.baseline_art_track_24)
                        setTitle(R.string.post_title_and_image_only)
                    }
                    ScreenshotModeViewModel.PostViewType.Compact -> {
                        setIcon(R.drawable.baseline_list_24)
                        setTitle(R.string.compact_post)
                    }
                }
            }

            generateScreenshot(adapter)
        }

        binding.bottomAppBar.setNavigationOnClickListener {
            dismiss()
        }
        binding.bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.toggle_post_view -> {
                    val currentConfig = requireNotNull(viewModel.screenshotConfig.value)
                    viewModel.screenshotConfig.value = currentConfig.copy(
                        postViewType = currentConfig.postViewType.nextValue,
                    )
                }
                R.id.screenshot_settings -> {
                    ScreenshotSettingsDialogFragment()
                        .showAllowingStateLoss(
                            childFragmentManager,
                            "ScreenshotSettingsDialogFragment",
                        )
                }
                R.id.gif -> {
                    val infographicsView = binding.zoomLayout.children.firstOrNull()
                    if (infographicsView != null) {
                        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        viewModel.generateGifToSave(infographicsView, "post_screenshot_$ts")
                    }
                }
            }

            true
        }

        viewModel.generatedImageUri.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()
                    binding.loadingOverlay.visibility = View.GONE
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                    binding.loadingOverlay.visibility = View.VISIBLE
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.loadingView.hideAll()

                    when (it.data.reason) {
                        ScreenshotModeViewModel.UriResult.Reason.Share ->
                            when (it.data.fileType) {
                                ScreenshotModeViewModel.UriResult.FileType.Gif ->
                                    shareUri(it.data.uri, MimeTypes.GIF)
                                ScreenshotModeViewModel.UriResult.FileType.Png ->
                                    shareUri(it.data.uri, MimeTypes.PNG)
                                ScreenshotModeViewModel.UriResult.FileType.Mp4 ->
                                    shareUri(it.data.uri, MimeTypes.MP4)
                            }
                        ScreenshotModeViewModel.UriResult.Reason.Save -> {
                            uriToSave = it.data.uri
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val fileName = "post_screenshot_$ts"

                            when (it.data.fileType) {
                                ScreenshotModeViewModel.UriResult.FileType.Gif ->
                                    createGifDocumentLauncher.launch(fileName)
                                ScreenshotModeViewModel.UriResult.FileType.Png ->
                                    createPngDocumentLauncher.launch(fileName)
                                ScreenshotModeViewModel.UriResult.FileType.Mp4 ->
                                    createMp4DocumentLauncher.launch(fileName)
                            }
                        }
                    }
                }
            }
        }
    }

    fun generateScreenshot() {
        val parent = (parentFragment as? PostFragment)

        if (parent == null) {
            dismiss()
            return
        }

        val adapter = parent.getAdapter()

        if (adapter == null) {
            dismiss()
            return
        }

        generateScreenshot(adapter)
    }

    private fun generateScreenshot(adapter: PostAdapter) {
        val context = requireContext()

        val screenshotStage = ScreenshotStageBinding.inflate(
            LayoutInflater.from(context),
            binding.zoomLayout,
            false,
        )

        var screenshotWidthDp = preferences.screenshotWidthDp
        screenshotWidthDp = screenshotWidthDp.coerceIn(100, 1000)

        val threadLinesDecoration = ModernThreadLinesDecoration(context, false)
        val screenshotWidth = Utils.convertDpToPixel(screenshotWidthDp.toFloat()).toInt()

        screenshotStage.root.updateLayoutParams<LayoutParams> {
            width = screenshotWidth
        }

        adapter.screenshotMaxWidth = screenshotWidth
        adapter.isScreenshoting = true

        var hasPost = false
        var hasNonPosts = false
        for (position in 0 until adapter.itemCount) {
            val isSelectedForScreenshot = adapter.isSelectedForScreenshot(position)
            if (!isSelectedForScreenshot) {
                continue
            }
            val isPost = adapter.isPost(position)
            if (isPost) {
                hasPost = true
            } else {
                hasNonPosts = true
            }
        }

        val screenshotConfig = viewModel.screenshotConfig.value?.copy(
            showPostDivider = hasPost && hasNonPosts,
        )

        adapter.screenshotConfig = screenshotConfig

        when (screenshotConfig?.postViewType) {
            ScreenshotModeViewModel.PostViewType.Full -> {
                screenshotStage.contentContainer.updatePadding(
                    top = context.getDimen(R.dimen.padding),
                )
            }
            ScreenshotModeViewModel.PostViewType.ImageOnly -> {
                screenshotStage.contentContainer.updatePadding(top = 0)
            }
            ScreenshotModeViewModel.PostViewType.TextOnly -> {
                screenshotStage.contentContainer.updatePadding(
                    top = context.getDimen(R.dimen.padding),
                )
            }
            ScreenshotModeViewModel.PostViewType.TitleOnly -> {
                screenshotStage.contentContainer.updatePadding(
                    top = context.getDimen(R.dimen.padding),
                )
            }
            ScreenshotModeViewModel.PostViewType.TitleAndImageOnly -> {
                screenshotStage.contentContainer.updatePadding(
                    top = context.getDimen(R.dimen.padding),
                )
            }
            ScreenshotModeViewModel.PostViewType.Compact -> {
                screenshotStage.contentContainer.updatePadding(
                    top = context.getDimen(R.dimen.padding),
                )
            }
            null -> {
                screenshotStage.contentContainer.updatePadding(
                    top = context.getDimen(R.dimen.padding),
                )
            }
        }

        for (position in 0 until adapter.itemCount) {
            val isSelectedForScreenshot = adapter.isSelectedForScreenshot(position)

            if (!isSelectedForScreenshot) {
                continue
            }

            val adapterViewType = adapter.getItemViewType(position)
            val vh = adapter.onCreateViewHolder(screenshotStage.contentContainer, adapterViewType)

            adapter.onBindViewHolder(vh, position)

            screenshotStage.contentContainer.addView(vh.itemView)

            Log.d(TAG, "Adding view at position $position")
        }
        adapter.isScreenshoting = false

        screenshotStage.contentContainer.decorator = threadLinesDecoration

        val dateScreenshots = preferences.dateScreenshots
        val screenshotWatermark = preferences.screenshotWatermark
        if (dateScreenshots || screenshotWatermark != ScreenshotWatermarkId.Off) {
            screenshotStage.contentContainer.addView(
                MaterialDivider(context),
            )
            ScreenshotBottomBarBinding.inflate(
                LayoutInflater.from(context),
                screenshotStage.contentContainer,
                true,
            ).apply {
                if (dateScreenshots) {
                    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    text.text = dateFormat.format(
                        Instant.now().atZone(ZoneId.systemDefault()).toLocalDate(),
                    )
                } else {
                    text.visibility = View.GONE
                }

                when (screenshotWatermark) {
                    ScreenshotWatermarkId.Lemmy -> {
                        watermark.setImageResource(R.drawable.ic_lemmy_24)
                    }
                    ScreenshotWatermarkId.Summit -> {
                        watermark.setImageResource(R.drawable.ic_logo_mono_24)
                    }
                    else -> {
                        watermark.visibility = View.GONE
                    }
                }
            }
        }

        binding.zoomLayout.removeAllViews()
        binding.zoomLayout.addView(screenshotStage.root)
        binding.zoomLayout.requestLayout()
    }

    private fun onCreateDocumentResult(result: Uri?) {
        result ?: return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val uriToSave = uriToSave ?: return@repeatOnLifecycle

                val context = requireContext()
                context.contentResolver.openInputStream(uriToSave).use a@{ ins ->
                    ins ?: return@a
                    context.contentResolver.openOutputStream(result).use { out ->
                        out ?: return@a

                        val buf = ByteArray(8192)
                        var length: Int
                        while (ins.read(buf).also { length = it } != -1) {
                            out.write(buf, 0, length)
                        }
                    }
                }

                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        REQUEST_KEY_RESULT to Result(
                            uriToSave,
                            MimeTypes.PNG,
                        ),
                    ),
                )
                dismiss()
            }
        }
    }
}
