package com.idunnololz.summit.preview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentImageViewerBinding
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.scrape.ImgurWebsiteAdapter
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.util.*
import com.idunnololz.summit.view.GalleryImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.io.FilenameUtils
import java.io.IOException

class ImageViewerFragment : BaseFragment<FragmentImageViewerBinding>() {

    companion object {

        private const val PERMISSION_REQUEST_EXTERNAL_WRITE = 1

        @Suppress("unused")
        private val TAG = ImageViewerFragment::class.java.canonicalName
    }

    private val args: ImageViewerFragmentArgs by navArgs()

    private lateinit var decorView: View
    private var showingUi = true

    private var url: String? = null
    private lateinit var fileName: String
    private var mimeType: String? = null

    private var downloadAndSaveImageDisposable: Disposable? = null

    private var websiteAdapterLoader: WebsiteAdapterLoader? = null

    private val offlineManager = OfflineManager.instance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        (activity as MainActivity).apply {
            setupForFragment<ImageViewerFragment>()
        }

        if (!RedditUtils.isUrlAGif(args.url)) {
            sharedElementEnterTransition = SharedElementTransition()
            sharedElementReturnTransition = SharedElementTransition()
        }

        setHasOptionsMenu(true)

        setBinding(FragmentImageViewerBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()

        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setupActionBar(R.string.image_viewer, true)

        requireMainActivity().let {
            val toolbar = it.binding.toolbar
            it.windowInsets.observe(viewLifecycleOwner, Observer {
                binding.rootView.getConstraintSet(R.id.normal)
                    .setMargin(
                        R.id.dismiss_warning,
                        ConstraintSet.TOP,
                        it.top + toolbar.layoutParams.height
                    )
                binding.rootView.getConstraintSet(R.id.exiting)
                    .setMargin(
                        R.id.dismiss_warning,
                        ConstraintSet.TOP,
                        it.top + toolbar.layoutParams.height
                    )
            })
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_image_viewer, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> {
                val context = context ?: return true
                // Here, thisActivity is the current activity
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_EXTERNAL_WRITE
                    )
                } else {
                    downloadImage()
                }
                return true
            }
            R.id.openInBrowser -> {
                val context = context ?: return true
                Utils.openExternalLink(context, args.url)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
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
            downloadAndSaveImageDisposable?.dispose()
            downloadAndSaveImageDisposable = FileDownloadHelper
                .downloadFile(
                    context,
                    fileName,
                    url,
                    it,
                    mimeType = mimeType
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ downloadResult ->
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
                }, { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
                    Snackbar.make(binding.rootView, R.string.error_downloading_image, Snackbar.LENGTH_LONG)
                        .show()
                })
        }, {
            Snackbar.make(binding.rootView, R.string.error_downloading_image, Snackbar.LENGTH_LONG).show()
        })
    }

    private fun loadImageFromUrl(url: String) {
        Log.d(TAG, "loadImageFromUrl: $url")

        offlineManager.fetchImageWithError(binding.rootView, url, {
            val tempSize = Size()
            offlineManager.calculateImageMaxSizeIfNeeded(it)
            offlineManager.getMaxImageSizeHint(it, tempSize)

            Glide.with(this)
                .load(it)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.loadingView.showErrorWithRetry(R.string.error_downloading_image)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.loadingView.hideAll()
                        if (resource is GifDrawable) {
                            binding.imageView.post {
                                resource.setVisible(true, true)
                            }
                        }
                        return false
                    }

                })
                .also {
                    if (tempSize.height != -1 && Utils.getScreenHeight(requireContext()) > tempSize.height * 4) {
                        it.override(tempSize.width * 4, tempSize.height * 4)
                    } else if (tempSize.height == -1) {
                        it.centerInside()
                    }
                }
                .into(binding.imageView)
        }, {
            binding.loadingView.showDefaultErrorMessageFor(it)
        })
    }
}