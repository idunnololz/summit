package com.idunnololz.summit.preview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.transition.Transition
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.webkit.MimeTypeMap
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.navArgs
import androidx.transition.TransitionManager
import coil.imageLoader
import coil.request.ImageRequest
import coil.target.Target
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentImageViewerBinding
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.createImageOrLinkActionsHandler
import com.idunnololz.summit.lemmy.utils.showAdvancedLinkOptions
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.scrape.ImgurWebsiteAdapter
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.BottomMenuContainer
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.InsetsHelper
import com.idunnololz.summit.util.InsetsProvider
import com.idunnololz.summit.util.SharedElementNames
import com.idunnololz.summit.util.SharedElementTransition
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.showAboveCutout
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.makeTransition
import com.idunnololz.summit.view.GalleryImageView
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class ImageViewerActivity :
    BaseActivity(), BottomMenuContainer, InsetsProvider by InsetsHelper(consumeInsets = false) {

    companion object {

        @Suppress("unused")
        private val TAG = ImageViewerActivity::class.java.canonicalName

        private const val EXIT_OFFSET_DP = 60f

        const val ErrorCustomDownloadLocation = 1234
    }

    private val args: ImageViewerActivityArgs by navArgs()

    private val viewModel: ImageViewerViewModel by viewModels()

    private var currentBottomMenu: BottomMenu? = null

    private var showingUi = true

    private var url: String? = null
    private lateinit var fileName: String
    private var mimeType: String? = null

    private var websiteAdapterLoader: WebsiteAdapterLoader? = null

    private lateinit var binding: FragmentImageViewerBinding

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var offlineManager: OfflineManager

    override val context: Context
        get() = this
    override val mainApplication: MainApplication
        get() = application as MainApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        postponeEnterTransition()

        super.onCreate(savedInstanceState)
        binding = FragmentImageViewerBinding.inflate(LayoutInflater.from(this))

        setContentView(binding.root)
        registerInsetsHandler(binding.root)

        showAboveCutout()

        onViewCreated()

        setSupportActionBar(binding.toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = ""

        binding.dummyAppBar.transitionName = SharedElementNames.AppBar
        binding.bottomNavigationView.transitionName = SharedElementNames.NavBar

        onInsetsChanged = { insets ->
            binding.toolbar.layoutParams = binding.toolbar.layoutParams.apply {
                (this as MarginLayoutParams).topMargin = insets.topInset
            }
        }

        window.enterTransition = Fade(Fade.IN).apply {
            duration = 200
            excludeTarget(R.id.dummy_image_view, true)
            excludeTarget(R.id.image_view, true)
        }
        window.exitTransition = Fade(Fade.OUT).apply {
            duration = 200
            excludeTarget(R.id.dummy_image_view, true)
            excludeTarget(R.id.image_view, true)
        }
        window.returnTransition = Fade(Fade.OUT).apply {
            duration = 200
            excludeTarget(R.id.dummy_image_view, true)
            excludeTarget(R.id.image_view, true)
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

                override fun onTransitionEnd(p0: Transition?) {}

                override fun onTransitionCancel(p0: Transition?) {}

                override fun onTransitionPause(p0: Transition?) {}

                override fun onTransitionResume(p0: Transition?) {}
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

        moreActionsHelper.downloadAndShareFile.observe(this) {
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

    fun onViewCreated() {
        if (intent.extras == null) {
            finish()
            return
        }

        insets.observe(this) {
            insetViewExceptTopAutomaticallyByPadding(this, binding.bottomBar)
        }

        val context = this

        binding.dummyImageView.transitionName = args.transitionName

        binding.loadingView.showProgressBar()
        binding.loadingView.setOnRefreshClickListener {
            loadImage(args.url)
        }

        loadImage(args.url)

        binding.imageView.postDelayed({
            startPostponedEnterTransition()
        }, 50,)

        moreActionsHelper.downloadResult.observe(this) {
            when (it) {
                is StatefulData.NotStarted -> {}
                is StatefulData.Error -> {
                    Snackbar.make(
                        getSnackbarContainer(),
                        R.string.error_downloading_image,
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
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
                            if (it is FileDownloadHelper.CustomDownloadLocationException) {
                                Snackbar
                                    .make(
                                        getSnackbarContainer(),
                                        R.string.error_downloading_image,
                                        Snackbar.LENGTH_LONG,
                                    )
                                    .setAction(R.string.downloads_settings) {
                                        setResult(ErrorCustomDownloadLocation)
                                        finish()
                                    }
                                    .show()
                            } else {
                                FirebaseCrashlytics.getInstance().recordException(it)
                                Snackbar
                                    .make(
                                        getSnackbarContainer(),
                                        R.string.error_downloading_image,
                                        Snackbar.LENGTH_LONG,
                                    )
                                    .show()
                            }
                        }
                }
            }
        }

        binding.shareButton.setOnClickListener {
            createImageOrLinkActionsHandler(
                args.url,
                moreActionsHelper,
                supportFragmentManager,
                args.mimeType,
            )(R.id.share_image)
        }
        binding.downloadButton.setOnClickListener {
            createImageOrLinkActionsHandler(
                args.url,
                moreActionsHelper,
                supportFragmentManager,
                args.mimeType,
            )(R.id.download)
        }
        binding.moreButton.setOnClickListener {

            showAdvancedLinkOptions(
                args.url,
                moreActionsHelper,
                supportFragmentManager,
                args.mimeType,
            )
        }
    }

    override fun onDestroy() {
        offlineManager.cancelFetch(binding.root)
        showSystemUI()
        websiteAdapterLoader?.destroy()
        super.onDestroy()
    }

    private fun downloadImage() {
        createImageOrLinkActionsHandler(
            args.url,
            moreActionsHelper,
            supportFragmentManager,
            args.mimeType,
        )(R.id.download)
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

        fileName = with(imageName ?: url) {
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

            override fun zoom(curZoom: Float) {
                this@ImageViewerActivity.hideUi()
            }

            var isExiting = false
            override fun overScroll(offX: Float, offY: Float, curZoom: Float) {
                val exitOffsetScaled = Utils.convertDpToPixel(EXIT_OFFSET_DP) / curZoom
                if (offY > exitOffsetScaled ||
                    offY < -exitOffsetScaled ||
                    offX > exitOffsetScaled ||
                    offX < -exitOffsetScaled
                ) {
                    if (!isExiting) {
                        isExiting = true
                    }
                } else {
                    if (isExiting) {
                        isExiting = false
                    }
                }
            }

            override fun overScrollEnd(): Boolean {
                if (isExiting) {
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
        binding.bottomBar.animate().translationY(0f)
    }

    private fun hideUi() {
        showingUi = false
        hideActionBar()
        hideSystemUI()
        binding.bottomBar.animate().translationY(binding.bottomBar.height.toFloat())
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(
            window,
            requireNotNull(window.decorView),
        ).let {
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showSystemUI() {
        WindowInsetsControllerCompat(
            window,
            requireNotNull(window.decorView),
        ).show(WindowInsetsCompat.Type.systemBars())
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
            binding.loadingView.showDefaultErrorMessageFor(
                it,
            )
        },)
    }

    private fun showActionBar() {
        TransitionManager.beginDelayedTransition(binding.root, makeTransition())

        binding.appBar.animate().translationY(0f)
    }

    private fun hideActionBar() {
        TransitionManager.beginDelayedTransition(binding.root, makeTransition())

        binding.appBar.animate().translationY((-binding.appBar.height).toFloat())
    }

    private fun getSnackbarContainer(): View = binding.contentView

    override fun showBottomMenu(bottomMenu: BottomMenu, expandFully: Boolean) {
        currentBottomMenu?.close()
        bottomMenu.show(
            bottomMenuContainer = this,
            bottomSheetContainer = binding.root,
            expandFully = expandFully,
        )
        currentBottomMenu = bottomMenu
    }
}
