package com.idunnololz.summit.preview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import coil.drawable.MovieDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.target.Target
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentImageViewerBinding
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.reddit.LemmyUtils
import com.idunnololz.summit.scrape.ImgurWebsiteAdapter
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.util.*
import com.idunnololz.summit.view.GalleryImageView
import dagger.hilt.android.AndroidEntryPoint
import org.apache.commons.io.FilenameUtils
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class ImageViewerFragment : BaseFragment<FragmentImageViewerBinding>() {

    companion object {

        private const val PERMISSION_REQUEST_EXTERNAL_WRITE = 1

        @Suppress("unused")
        private val TAG = ImageViewerFragment::class.java.canonicalName
    }

    private val args: ImageViewerFragmentArgs by navArgs()

    private val viewModel: ImageViewerViewModel by viewModels()

    private lateinit var decorView: View
    private var showingUi = true

    private var url: String? = null
    private lateinit var fileName: String
    private var mimeType: String? = null

    private var websiteAdapterLoader: WebsiteAdapterLoader? = null

    @Inject
    lateinit var offlineManager: OfflineManager

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                downloadImage()
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.error_downloading_image_permission_denied,
                    Snackbar.LENGTH_LONG,
                ).setAction(R.string.help) {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(LinkUtils.APP_PERMISSIONS_HELP_ARTICLE)
                    Utils.safeLaunchExternalIntent(requireContext(), i)
                }.show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        (activity as MainActivity).apply {
            setupForFragment<ImageViewerFragment>()
        }

        if (!LemmyUtils.isUrlAGif(args.url)) {
            sharedElementEnterTransition = SharedElementTransition()
            sharedElementReturnTransition = SharedElementTransition()
        }

        setBinding(FragmentImageViewerBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()

        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        (activity as? MainActivity)?.setupActionBar(R.string.image_viewer, true)

        requireMainActivity().let {
            val toolbarHeight = it.getToolbarHeight()
            it.windowInsets.observe(viewLifecycleOwner) {
                binding.rootView.getConstraintSet(R.id.normal)
                    .setMargin(
                        R.id.dismiss_warning,
                        ConstraintSet.TOP,
                        it.top + toolbarHeight
                    )
                binding.rootView.getConstraintSet(R.id.exiting)
                    .setMargin(
                        R.id.dismiss_warning,
                        ConstraintSet.TOP,
                        it.top + toolbarHeight
                    )
            }
        }

        binding.loadingView.showProgressBar()
        binding.loadingView.setOnRefreshClickListener {
            loadImage(args.url)
        }

        val parent = activity as MainActivity

        decorView = parent.window.decorView

        ViewCompat.setTransitionName(binding.imageView, "image_view")

        loadImage(args.url)

        binding.imageView.post {
            startPostponedEnterTransition()
        }

        viewModel.downloadResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.NotStarted -> {}
                is StatefulData.Error -> {}
                is StatefulData.Loading -> {}
                is StatefulData.Success -> {
                    it.data
                        .onSuccess { downloadResult ->
                            try {
                                val uri = downloadResult.uri
                                val mimeType = downloadResult.mimeType

                                val snackbarMsg = getString(R.string.image_saved_format, downloadResult.uri)
                                Snackbar.make(
                                    requireMainActivity().getSnackbarContainer(),
                                    snackbarMsg,
                                    Snackbar.LENGTH_LONG
                                ).setAction(R.string.view) {
                                    if (!isAdded) {
                                        return@setAction
                                    }

                                    Utils.safeLaunchExternalIntentWithErrorDialog(
                                        context,
                                        parentFragmentManager,
                                        Intent(Intent.ACTION_VIEW).apply {
                                            flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            setDataAndType(uri, mimeType)
                                        })
                                }.show()
                            } catch (e: IOException) {/* do nothing */
                            }
                        }
                        .onFailure {
                            FirebaseCrashlytics.getInstance().recordException(it)
                            Snackbar.make(binding.rootView, R.string.error_downloading_image, Snackbar.LENGTH_LONG)
                                .show()
                        }
                }
            }
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_image_viewer, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.save -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            )
                        } else {
                            downloadImage()
                        }
                        true
                    }
                    R.id.openInBrowser -> {
                        Utils.openExternalLink(context, args.url)
                        true
                    }
                    else -> false
                }

        })
    }

    override fun onDestroyView() {
        offlineManager.cancelFetch(binding.rootView)

        super.onDestroyView()
    }

    override fun onDestroy() {
        (requireActivity() as MainActivity).showSystemUI(false)
        websiteAdapterLoader?.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_EXTERNAL_WRITE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadImage()
                }
            }
        }
    }

    /**
     * @param forceLoadAsImage is true, will ignore the url format or domain and download the page
     * and pretend it's an image/gif
     */
    private fun loadImage(url: String, forceLoadAsImage: Boolean = false) {
        val uri = Uri.parse(url)
        if (uri.host == "imgur.com" && !forceLoadAsImage && uri.encodedPath?.endsWith(".png") != true) {
            websiteAdapterLoader = WebsiteAdapterLoader().apply {
                add(ImgurWebsiteAdapter(url), url, Utils.hashSha256(url))
                setOnEachAdapterLoadedListener a@{
                    if (it is ImgurWebsiteAdapter) {
                        val rootView = binding.rootView ?: return@a

                        if (it.isSuccess()) {
                            rootView.post {
                                if (!isAdded) return@post

                                if (it.get().wasUrlRawGif) {
                                    loadImage(url, forceLoadAsImage = true)
                                } else {
                                    loadImage(it.get().url)
                                }
                            }
                        } else {
                            rootView.post {
                                if (!isAdded) return@post

                                binding.loadingView.showDefaultErrorMessageFor(it.error)

                                showUi()
                            }
                        }
                    }
                }
            }.load()
            return
        }

        this.url = url
        val imageName = args.title
        mimeType = args.mimeType

        val title =
            with(imageName ?: FilenameUtils.getName(url) ?: getString(R.string.image_viewer)) {
                substring(lastIndexOf('/') + 1)
            }

        fileName = FilenameUtils.getName(url) ?: "unknown"

        (activity as? MainActivity)?.setupActionBar(title, true)

        binding.imageView.callback = object : GalleryImageView.Callback {
            override fun togggleUi() {
                this@ImageViewerFragment.togggleUi()
            }

            override fun showUi() {
                this@ImageViewerFragment.showUi()
            }

            override fun hideUi() {
                this@ImageViewerFragment.hideUi()
            }

            var currentState = 0
            override fun overScroll(offX: Float, offY: Float) {
                if (offY > Utils.convertDpToPixel(80f)) {
                    if (currentState != R.id.exiting) {
                        currentState = R.id.exiting
                        binding.rootView.transitionToState(R.id.exiting)
                    }
                } else {
                    if (currentState != R.id.normal) {
                        currentState = R.id.normal
                        binding.rootView.transitionToState(R.id.normal)
                    }
                }
            }

            override fun overScrollEnd(): Boolean {
                if (currentState == R.id.exiting) {
                    requireActivity().onBackPressed()
                    return true
                }

                return false
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.imageView) { v, insets ->
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = insets.systemWindowInsetTop
            insets.consumeSystemWindowInsets()
        }

        loadImageFromUrl(url)
    }

    private fun togggleUi() {
        if (showingUi) {
            hideUi()
        } else {
            showUi()
        }
    }

    private fun showUi() {
        showingUi = true
        (activity as? MainActivity)?.showActionBar()
        (activity as? MainActivity)?.showSystemUI(true)
    }

    private fun hideUi() {
        showingUi = false
        (activity as? MainActivity)?.hideActionBar(true)
        (activity as? MainActivity)?.hideSystemUI()
    }

    private fun downloadImage() {
        val context = context ?: return
        offlineManager.fetchImageWithError(binding.rootView, url, {
            viewModel.downloadFile(
                context,
                fileName,
                url,
                it,
                mimeType = mimeType
            )
        }, {
            Snackbar.make(
                binding.rootView,
                R.string.error_downloading_image,
                Snackbar.LENGTH_LONG
            ).show()
        })
    }

    private fun loadImageFromUrl(url: String) {
        Log.d(TAG, "loadImageFromUrl: $url")

        offlineManager.fetchImageWithError(binding.rootView, url, {
            val tempSize = Size()
            offlineManager.calculateImageMaxSizeIfNeeded(it)
            offlineManager.getMaxImageSizeHint(it, tempSize)

            val request = ImageRequest.Builder(binding.imageView.context)
                .data(it)
                .target(object : Target {
                    override fun onError(error: Drawable?) {
                        super.onError(error)
                        binding.loadingView.showErrorWithRetry(R.string.error_downloading_image)
                    }

                    override fun onStart(placeholder: Drawable?) {
                        super.onStart(placeholder)
                    }

                    override fun onSuccess(result: Drawable) {
                        super.onSuccess(result)

                        binding.loadingView.hideAll()

                        binding.imageView.setImageDrawable(result)

                        if (result is MovieDrawable) {
                            result.start()
                        }
                    }
                })
                .build()
            binding.imageView.context.imageLoader.enqueue(request)
        }, {
            binding.loadingView.showDefaultErrorMessageFor(it)
        })
    }
}