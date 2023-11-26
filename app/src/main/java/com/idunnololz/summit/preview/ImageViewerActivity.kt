package com.idunnololz.summit.preview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Transition
import android.util.Log
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.navArgs
import coil.imageLoader
import coil.request.ImageRequest
import coil.target.Target
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentImageViewerBinding
import com.idunnololz.summit.links.LinkType
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.GlobalSettings
import com.idunnololz.summit.scrape.ImgurWebsiteAdapter
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.showAboveCutout
import com.idunnololz.summit.view.GalleryImageView
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class ImageViewerActivity : BaseActivity() {

    companion object {

        private const val PERMISSION_REQUEST_EXTERNAL_WRITE = 1

        @Suppress("unused")
        private val TAG = ImageViewerActivity::class.java.canonicalName

        private const val EXIT_OFFSET_DP = 60f
    }

    private val args: ImageViewerActivityArgs by navArgs()

    private val viewModel: ImageViewerViewModel by viewModels()

    private lateinit var decorView: View
    private var showingUi = true

    private var url: String? = null
    private lateinit var fileName: String
    private var mimeType: String? = null

    private var websiteAdapterLoader: WebsiteAdapterLoader? = null

    private lateinit var binding: FragmentImageViewerBinding

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
                    Utils.safeLaunchExternalIntent(this, i)
                }.show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onCreate(savedInstanceState)
        binding = FragmentImageViewerBinding.inflate(LayoutInflater.from(this))

        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        showAboveCutout()

        onViewCreated(binding.root, savedInstanceState)

        setupActionBar(args.title, true)

        binding.dummyAppBar.transitionName = SharedElementNames.AppBar
        binding.bottomNavigationView.transitionName = SharedElementNames.NavBar

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { _, insets ->
            val insetsCompat = WindowInsetsCompat(insets)
            val insetsSystemBars = insetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.toolbar.layoutParams = binding.toolbar.layoutParams.apply {
                (this as MarginLayoutParams).topMargin = insetsSystemBars.top
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && isLightTheme()) {
                binding.statusBarBg.layoutParams = binding.statusBarBg.layoutParams.apply {
                    height = insetsSystemBars.top
                }
                binding.statusBarBg.visibility = View.VISIBLE
            } else {
                binding.statusBarBg.visibility = View.GONE
            }

//            binding.imageView.updateLayoutParams<MarginLayoutParams> {
//                topMargin = insetsSystemBars.top
//            }
//            binding.dummyImageView.updateLayoutParams<MarginLayoutParams> {
//                topMargin = insetsSystemBars.top
//            }

            insets
        }
//        if (!LemmyUtils.isUrlAGif(args.url)) {
//            sharedElementEnterTransition = SharedElementTransition()
//            sharedElementReturnTransition = SharedElementTransition()
//        }

        window.enterTransition = Fade(Fade.IN).apply {
            duration = 200
            excludeTarget(R.id.dummy_image_view, true)
            excludeTarget(R.id.imageView, true)
        }
        window.exitTransition = Fade(Fade.OUT).apply {
            duration = 200
            excludeTarget(R.id.dummy_image_view, true)
            excludeTarget(R.id.imageView, true)
        }
        window.returnTransition = Fade(Fade.OUT).apply {
            duration = 200
            excludeTarget(R.id.dummy_image_view, true)
            excludeTarget(R.id.imageView, true)
        }
        window.sharedElementEnterTransition = SharedElementTransition()
        window.sharedElementReturnTransition = SharedElementTransition()
//        window.sharedElementsUseOverlay = false

        binding.dummyImageView.visibility = View.VISIBLE
        binding.imageView.visibility = View.INVISIBLE
        window.sharedElementEnterTransition.addListener(
            object : Transition.TransitionListener {
                override fun onTransitionStart(p0: Transition?) {
                }

                override fun onTransitionEnd(p0: Transition?) {
                    binding.dummyImageView.post {
                        binding.dummyImageView.transitionName = null
                        binding.imageView.transitionName = args.transitionName

                        binding.dummyImageView.visibility = View.GONE
                        binding.imageView.visibility = View.VISIBLE
                    }
                }

                override fun onTransitionCancel(p0: Transition?) {
                }

                override fun onTransitionPause(p0: Transition?) {
                }

                override fun onTransitionResume(p0: Transition?) {
                }
            },
        )
        window.sharedElementExitTransition.addListener(
            object : Transition.TransitionListener {
                override fun onTransitionStart(p0: Transition?) {
                    binding.appBar.animate()
                        .translationY(-200f)
                }

                override fun onTransitionEnd(p0: Transition?) {
                }

                override fun onTransitionCancel(p0: Transition?) {
                }

                override fun onTransitionPause(p0: Transition?) {
                }

                override fun onTransitionResume(p0: Transition?) {
                }
            },
        )

        if (args.transitionName == null) {
            binding.dummyImageView.post {
                binding.dummyImageView.transitionName = null
                binding.imageView.transitionName = args.transitionName

                binding.dummyImageView.visibility = View.GONE
                binding.imageView.visibility = View.VISIBLE
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            supportFinishAfterTransition()
        }

        viewModel.downloadAndShareFile.observe(this) {
            when (it) {
                is StatefulData.Error -> {}
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(it.data.toString())
                        ?: "image/jpeg"

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, it.data)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }
            }
        }
    }

    fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = this

        binding.dummyImageView.transitionName = args.transitionName

        binding.loadingView.showProgressBar()
        binding.loadingView.setOnRefreshClickListener {
            loadImage(args.url)
        }

        decorView = window.decorView

        loadImage(args.url)

        binding.imageView.postDelayed({
            startPostponedEnterTransition()
        }, 50,)

        viewModel.downloadResult.observe(this) {
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
                                    getSnackbarContainer(),
                                    snackbarMsg,
                                    Snackbar.LENGTH_LONG,
                                ).setAction(R.string.view) {
                                    Utils.safeLaunchExternalIntentWithErrorDialog(
                                        context,
                                        supportFragmentManager,
                                        Intent(Intent.ACTION_VIEW).apply {
                                            flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            setDataAndType(uri, mimeType)
                                        },
                                    )
                                }.show()
                            } catch (e: IOException) { /* do nothing */
                            }
                        }
                        .onFailure {
                            FirebaseCrashlytics.getInstance().recordException(it)
                            Snackbar.make(getSnackbarContainer(), R.string.error_downloading_image, Snackbar.LENGTH_LONG)
                                .show()
                        }
                }
            }
        }

        addMenuProvider(
            object : MenuProvider {
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
                        R.id.share -> {
                            if (GlobalSettings.shareImagesDirectly) {
                                downloadAndShareImage(args.url)
                            } else {
                                Utils.shareLink(this@ImageViewerActivity, args.url)
                            }
                            true
                        }
                        R.id.copy_link -> {
                            Utils.copyToClipboard(this@ImageViewerActivity, args.url)
                            true
                        }
                        R.id.openInBrowser -> {
                            onLinkClick(args.url, null, LinkType.Action)
                            true
                        }
                        else -> false
                    }
            },
        )
    }
    override fun onDestroy() {
        offlineManager.cancelFetch(binding.root)
        showSystemUI()
        websiteAdapterLoader?.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
                        val rootView = binding.root ?: return@a

                        if (it.isSuccess()) {
                            rootView.post {
                                if (it.get().wasUrlRawGif) {
                                    loadImage(url, forceLoadAsImage = true)
                                } else {
                                    loadImage(it.get().url)
                                }
                            }
                        } else {
                            rootView.post {
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

        fileName = with(imageName ?: url ?: "unknown") {
            val s = substring(lastIndexOf('/') + 1)
            val indexOfDot = s.lastIndexOf('.')
            if (indexOfDot != -1) {
                s
            } else {
                if (url != null) {
                    val urlHasExtension = url.lastIndexOf(".") != -1
                    if (urlHasExtension) {
                        s + url.substring(url.lastIndexOf("."))
                    } else {
                        s
                    }
                } else {
                    s
                }
            }
        }

        setupActionBar(title, true)

        binding.imageView.callback = object : GalleryImageView.Callback {
            override fun togggleUi() {
                this@ImageViewerActivity.togggleUi()
            }

            override fun showUi() {
                this@ImageViewerActivity.showUi()
            }

            override fun hideUi() {
                this@ImageViewerActivity.hideUi()
            }

            var currentState = 0
            override fun overScroll(offX: Float, offY: Float, curZoom: Float) {
                val exitOffsetScaled = Utils.convertDpToPixel(EXIT_OFFSET_DP) / curZoom
                if (offY > exitOffsetScaled ||
                    offY < -exitOffsetScaled ||
                    offX > exitOffsetScaled ||
                    offX < -exitOffsetScaled
                ) {
                    if (currentState != R.id.exiting) {
                        currentState = R.id.exiting
                    }
                } else {
                    if (currentState != R.id.normal) {
                        currentState = R.id.normal
                    }
                }
            }

            override fun overScrollEnd(): Boolean {
                if (currentState == R.id.exiting) {
                    supportFinishAfterTransition()
                    return true
                }

                return false
            }
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
        showActionBar()
        showSystemUI()
    }

    private fun hideUi() {
        showingUi = false
        hideActionBar(true)
        hideSystemUI()
    }

    fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(
            window,
            requireNotNull(window.decorView),
        ).let {
            it.isAppearanceLightStatusBars
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(
            window,
            requireNotNull(window.decorView),
        ).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun downloadImage() {
        offlineManager.fetchImageWithError(binding.root, url, {
            viewModel.downloadFile(
                this,
                fileName,
                url,
                it,
                mimeType = mimeType,
            )
        }, {
            Snackbar.make(
                getSnackbarContainer(),
                R.string.error_downloading_image,
                Snackbar.LENGTH_LONG,
            ).show()
        },)
    }

    private fun loadImageFromUrl(url: String) {
        Log.d(TAG, "loadImageFromUrl: $url")

        offlineManager.fetchImageWithError(binding.root, url, {
//            if (!isBindingAvailable()) return@fetchImageWithError

            val tempSize = Size()
            offlineManager.getMaxImageSizeHint(it, tempSize)

            val request = ImageRequest.Builder(binding.imageView.context)
                .data(it)
                .allowHardware(false)
                .target(
                    object : Target {
                        override fun onError(error: Drawable?) {
                            super.onError(error)
                            binding.loadingView.showErrorWithRetry(R.string.error_downloading_image)
                        }

                        override fun onStart(placeholder: Drawable?) {
                            super.onStart(placeholder)
                        }

                        override fun onSuccess(result: Drawable) {
                            super.onSuccess(result)

                            startPostponedEnterTransition()
                            binding.loadingView.hideAll()

                            binding.imageView.setImageDrawable(result)
                            binding.dummyImageView.setImageDrawable(result)

                            if (result is Animatable) {
                                result.start()
                            }
                        }
                    },
                )
                .build()
            binding.imageView.context.imageLoader.enqueue(request)
        }, {
            binding.loadingView.showDefaultErrorMessageFor(it)
        },)
    }

    fun setupActionBar(
        title: CharSequence?,
        showUp: Boolean,
        usingTabsHint: Boolean = false,
        animateActionBarIn: Boolean = true,
        scrollFlags: Int? = null,
        resetFabState: Boolean = true,
    ) {
        setSupportActionBar(binding.toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.title = title
    }

    fun showActionBar() {
        hideActionBar(false)

        val supportActionBar = supportActionBar ?: return
        if (supportActionBar.isShowing) return
        supportActionBar.show()

        binding.statusBarBg.animate().alpha(1f)
    }

    fun hideActionBar(hideToolbar: Boolean = true, hideStatusBar: Boolean = false) {
        if (hideToolbar) {
            val supportActionBar = supportActionBar ?: return
            if (!supportActionBar.isShowing) {
                return
            }
            supportActionBar.hide()
        }
        binding.statusBarBg.animate().alpha(0f)
    }
    fun getSnackbarContainer(): View = binding.contentView

    fun downloadAndShareImage(url: String) {
        viewModel.downloadAndShareImage(url)
    }
}
