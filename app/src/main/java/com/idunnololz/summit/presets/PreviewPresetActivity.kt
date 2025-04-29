package com.idunnololz.summit.presets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigationrail.NavigationRailView
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.databinding.ActivityPreviewThemeBinding
import com.idunnololz.summit.databinding.PresetPreviewScreenBinding
import com.idunnololz.summit.databinding.PresetPreviewScreenContainerBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.main.CommunitySelectorController
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.main.MainFragmentArgs
import com.idunnololz.summit.main.NavBarController
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.SharedPreferencesManager
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.SummitActivity
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbarForActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

class TakePreviewScreenshotContract :
    ActivityResultContract<Unit, PreviewScreenshotResult?>() {

    @CallSuper
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, PreviewPresetActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): PreviewScreenshotResult? {
        return intent?.getParcelableExtra("result")
    }
}

@Parcelize
data class PreviewScreenshotResult(
    val phonePreviewSs: Uri,
    val tabletPreviewSs: Uri,
) : Parcelable

@AndroidEntryPoint
class PreviewPresetActivity : SummitActivity() {

    private lateinit var binding: ActivityPreviewThemeBinding

    private var currentBottomMenu: BottomMenu? = null

    override val context: Context
        get() = this
    override val mainApplication: MainApplication
        get() = application as MainApplication
    override val activity: BaseActivity
        get() = this
    override lateinit var navBarController: NavBarController
    override var moreActionsHelper: MoreActionsHelper
        get() = TODO("Not yet implemented")
        set(value) {}
    override var lockUiOpenness: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var currentNavController: NavController?
        get() = TODO("Not yet implemented")
        set(value) {}

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var sharedPreferencesManager: SharedPreferencesManager

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var directoryHelper: DirectoryHelper

    override fun onCreate(savedInstanceState: Bundle?) {
//        sharedPreferencesManager.useTempPreferences = true
//        preferences.updateWhichSharedPreference()
//        preferenceManager.updateCurrentPreferences(
//            accountManager.currentAccount.value, force = true)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        binding = ActivityPreviewThemeBinding.inflate(LayoutInflater.from(this))

        Navigation.setViewNavController(binding.root, NavController(this))

        setContentView(binding.root)

//        registerInsetsHandler(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarsInsets = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars(),
            )
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()) // keyboard
            val imeHeight = imeInsets.bottom
            val topInset = systemBarsInsets.top
            val bottomInset = max(systemBarsInsets.bottom, imeHeight)
            val leftInset = 0
            val rightInset = 0

            val mainLeftInset = systemBarsInsets.left
            val mainRightInset = systemBarsInsets.right

            binding.root.setPadding(
                leftInset,
                0,
                rightInset,
                0,
            )
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topInset
                leftMargin = leftInset
                rightMargin = rightInset
            }
            binding.mainContent.setPadding(
                leftInset,
                0,
                rightInset,
                bottomInset,
            )

            WindowInsetsCompat.CONSUMED
        }

        setupToolbarForActivity(binding.toolbar, getString(R.string.take_screenshot))

        navBarController = NavBarController(
            context = this,
            contentView = binding.root,
            lifecycleOwner = this,
            onNavBarChanged = {},
            getWindowBounds = {
                WindowMetricsCalculator.getOrCreate()
                    .computeCurrentWindowMetrics(activity)
                    .bounds
            },
        )

        bindViews()
    }

    private fun bindViews() = with(binding) {
        val screenBounds = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this@PreviewPresetActivity)
            .bounds

        val tabletPreviewBinding = newDevicePreview(
            previewsContainer = previewsContainer,
            dimensionRatio = "1:1",
        ).apply {
            root.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginEnd = Utils.convertDpToPixel(64f).toInt()
            }
        }
        val phonePreviewBinding = newDevicePreview(
            previewsContainer = previewsContainer,
            dimensionRatio = "6:13",
        ).apply {
            root.layoutParams = FrameLayout.LayoutParams(
                (screenBounds.width() * 0.45f).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = Utils.convertDpToPixel(64f).toInt()

                gravity = Gravity.END or Gravity.BOTTOM
            }
        }

        previewsContainer.addView(tabletPreviewBinding.root)
        previewsContainer.addView(phonePreviewBinding.root)

        lifecycleScope.launch(Dispatchers.Main) {
            stageForScreen(
                presetPreviewStage = tabletPreviewBinding,
                preferences = preferences,
                screenWidthDp = 820,
                screenHeightDp = 820,
            ).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = Utils.convertDpToPixel(64f).toInt()
                }
            }
            stageForScreen(
                presetPreviewStage = phonePreviewBinding,
                preferences = preferences,
                screenWidthDp = 360,
                screenHeightDp = 780,
            ).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (screenBounds.width() * 0.45f).toInt(),
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = Utils.convertDpToPixel(64f).toInt()

                    gravity = Gravity.END or Gravity.BOTTOM
                }
            }
            previewsContainer.requestLayout()
        }

        binding.captureButton.setOnClickListener {
            val tabletPreview = tabletPreviewBinding.zoomLayout
            val phonePreview = phonePreviewBinding.zoomLayout
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(Date())

            listOf(tabletPreviewBinding, phonePreviewBinding).forEach {
                it.flash.alpha = 1f
                it.flash.animate()
                    .alpha(0f)
            }

            lifecycleScope.launch(Dispatchers.Default) {
                val tabletPreviewSs = generateImageForView(tabletPreview, "tablet_$ts")
                val phonePreviewSs = generateImageForView(phonePreview, "phone_$ts")

                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(
                            "result",
                            PreviewScreenshotResult(
                                phonePreviewSs = phonePreviewSs,
                                tabletPreviewSs = tabletPreviewSs,
                            ),
                        )
                    },
                )
                finish()
            }
        }
    }

    private suspend fun generateImageForView(view: View, fileName: String): Uri {
        val bitmap = createBitmap(view.width, view.height)
        Canvas(bitmap).apply {
            view.draw(this)
        }

        return runInterruptible(Dispatchers.IO) {
            File(directoryHelper.imagesDir, "Preset_Preview_$fileName.jpg").apply {
                outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
                }
            }.toUri()
        }
    }

    override fun showBottomMenu(bottomMenu: BottomMenu, expandFully: Boolean) {
        currentBottomMenu?.close()
        bottomMenu.show(
            bottomMenuContainer = this,
            bottomSheetContainer = binding.root,
            expandFully = expandFully,
        )
        currentBottomMenu = bottomMenu
    }

    override fun runOnReady(lifecycleOwner: LifecycleOwner, cb: () -> Unit) {
        cb.invoke()
    }

    override fun insetViewAutomaticallyByPaddingAndNavUi(
        lifecycleOwner: LifecycleOwner,
        rootView: View,
        applyLeftInset: Boolean,
        applyTopInset: Boolean,
        applyRightInset: Boolean,
        applyBottomInset: Boolean,
    ) {
        insetViewAutomaticallyByPadding(lifecycleOwner, rootView, 0)
    }

    override fun launchPage(
        page: PageRef,
        switchToNativeInstance: Boolean,
        preferMainFragment: Boolean,
    ) {
        TODO("Not yet implemented")
    }

    override fun showCommunitySelector(
        currentCommunityRef: CommunityRef?,
    ): CommunitySelectorController {
        TODO("Not yet implemented")
    }

    override fun openImage(
        sharedElement: View?,
        appBar: View?,
        title: String?,
        url: String,
        mimeType: String?,
        urlAlt: String?,
        mimeTypeAlt: String?,
    ) {
        TODO("Not yet implemented")
    }

    override fun showCommunityInfo(communityRef: CommunityRef) {
        TODO("Not yet implemented")
    }

    private fun newDevicePreview(
        previewsContainer: ViewGroup,
        dimensionRatio: String,
    ): PresetPreviewScreenContainerBinding {
        val context = this

        return PresetPreviewScreenContainerBinding.inflate(
            LayoutInflater.from(context),
            previewsContainer,
            false,
        ).apply {
            zoomLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                this.dimensionRatio = dimensionRatio
            }
        }
    }

    private suspend fun stageForScreen(
        presetPreviewStage: PresetPreviewScreenContainerBinding,
        preferences: Preferences,
        screenWidthDp: Int,
        screenHeightDp: Int,
    ): MaterialCardView = withContext(Dispatchers.Main) {
        val context = this@PreviewPresetActivity
        val asyncLayoutInflater = AsyncLayoutInflater(context)

        val screenWidthPx = Utils.convertDpToPixel(screenWidthDp.toFloat()).toInt()
        val screenHeightPx = Utils.convertDpToPixel(screenHeightDp.toFloat()).toInt()

        presetPreviewStage.zoomLayout.removeAllViews()

        val presetPreviewScreenBinding = withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { cont ->
                asyncLayoutInflater.inflate(
                    R.layout.preset_preview_screen,
                    presetPreviewStage.zoomLayout,
                ) { view, resid, parent ->
                    cont.resumeWith(
                        Result.success(
                            PresetPreviewScreenBinding.bind(
                                view,
                            ),
                        ),
                    )
                }
            }
        }.apply {
            previewFragmentContainer.id = View.generateViewId()
            this.root.updateLayoutParams<LayoutParams> {
                width = screenWidthPx
                height = screenHeightPx
            }
            val navBarController = NavBarController(
                context = context,
                contentView = this.navBarContainer,
                lifecycleOwner = this@PreviewPresetActivity,
                onNavBarChanged = { navBar ->
                },
                getWindowBounds = {
                    Rect(0, 0, screenWidthPx, screenHeightPx)
                },
            )
            navBarController.setup()
            navBarController.onPreferencesChanged(preferences = preferences)

            if (navBarController.navBar is NavigationRailView) {
                this.mainContentContainer.apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                this.navBarContainer.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    height = LinearLayout.LayoutParams.MATCH_PARENT
                }
                this.mainContentContainer.removeView(this.navBarContainer)
                this.mainContentContainer.addView(this.navBarContainer, 0)
                this.contentContainer.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = 0
                    height = LinearLayout.LayoutParams.MATCH_PARENT
                    weight = 1f
                }
            } else {
                this.mainContentContainer.apply {
                    orientation = LinearLayout.VERTICAL
                }
                this.navBarContainer.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    height = LinearLayout.LayoutParams.WRAP_CONTENT
                }
                this.mainContentContainer.removeView(this.navBarContainer)
                this.mainContentContainer.addView(this.navBarContainer)
                this.contentContainer.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    height = 0
                    weight = 1f
                }
            }
        }
        presetPreviewStage.zoomLayout.addView(presetPreviewScreenBinding.rootView)

        presetPreviewScreenBinding.previewFragmentContainer.post {
            binding.root.findViewById<View>(presetPreviewScreenBinding.previewFragmentContainer.id)
                ?: return@post

            supportFragmentManager.beginTransaction()
                .replace(
                    presetPreviewScreenBinding.previewFragmentContainer.id,
                    MainFragment().apply {
                        arguments = MainFragmentArgs(isPreview = true).toBundle()
                    },
                )
                .commitNowAllowingStateLoss()
        }

        presetPreviewStage.root
    }
}
