package com.idunnololz.summit.lemmy.person

import android.os.Bundle
import android.text.Spannable
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.text.buildSpannedString
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.load
import coil3.request.allowHardware
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.info.isPersonBlocked
import com.idunnololz.summit.account.loadProfileImageOrDefault
import com.idunnololz.summit.account.toPersonRef
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentPersonBinding
import com.idunnololz.summit.lemmy.LemmyHeaderHelper.Companion.NEW_PERSON_DURATION
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.community.SlidingPaneController
import com.idunnololz.summit.lemmy.getAccountAgeString
import com.idunnololz.summit.lemmy.post.PostFragmentDirections
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.userTags.AddOrEditUserTagDialogFragment
import com.idunnololz.summit.lemmy.userTags.UserTagsManager
import com.idunnololz.summit.lemmy.utils.SortTypeMenuHelper
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.actions.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.spans.RoundedBackgroundSpan
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.PrettyPrintUtils.defaultDecimalFormat
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDimenFromAttribute
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.tsToConcise
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonTabbedFragment : BaseFragment<FragmentPersonBinding>(), SignInNavigator {

  companion object {
    private const val TAG = "PersonTabbedFragment"
  }

  private val args by navArgs<PersonTabbedFragmentArgs>()

  @Inject
  lateinit var offlineManager: OfflineManager

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var avatarHelper: AvatarHelper

  @Inject
  lateinit var accountManager: AccountManager

  @Inject
  lateinit var moreActionsHelper: MoreActionsHelper

  @Inject
  lateinit var userTagsManager: UserTagsManager

  val viewModel: PersonTabbedViewModel by viewModels()
  var slidingPaneController: SlidingPaneController? = null

  private var isAnimatingTitleIn: Boolean = false
  private var isAnimatingTitleOut: Boolean = false

  private val personRef
    get() = args.personRef
      ?: accountManager.currentAccount.asAccount?.toPersonRef()

  private var consumedArgs = false

  enum class Screen {
    Posts,
    Comments,
    About,
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.showCurrentAccount.value = args.personRef == null
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentPersonBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()
    val mainActivity = requireSummitActivity()

    with(binding) {
      requireSummitActivity().apply {
        setupToolbar(
          toolbar,
          "",
        )

        insetViewAutomaticallyByPaddingAndNavUi(
          viewLifecycleOwner,
          coordinatorLayout,
          applyTopInset = false,
        )
        insets.observe(viewLifecycleOwner) {
          val newToolbarHeight =
            context.getDimenFromAttribute(androidx.appcompat.R.attr.actionBarSize).toInt() +
              it.topInset

          bannerDummy.updateLayoutParams<MarginLayoutParams> {
            topMargin = -it.topInset
          }

          coordinatorLayout.updatePadding(top = it.topInset)
          collapsingToolbarLayout.scrimVisibleHeightTrigger =
            (newToolbarHeight + Utils.convertDpToPixel(16f)).toInt()
          bannerGradient.updateLayoutParams<ViewGroup.LayoutParams> {
            height = newToolbarHeight
          }
        }
      }

      onPersonChanged()

      fab.hide()
      tabLayoutContainer.visibility = View.GONE

      appBar.addOnOffsetChangedListener { appBar, offset ->
        val topInset = mainActivity.insets.value?.topInset ?: 0

        val fixedTotalRange = appBar.totalScrollRange - topInset

        val progress = min(1f, abs(offset.toFloat() / fixedTotalRange))
        val scrimEndProgress = 0.7f

        bannerContainer.alpha = max(0f, 1f - progress / scrimEndProgress)
      }

      if (args.personRef == null) {
        viewModel.currentAccountView.observe(viewLifecycleOwner) {
          it.loadProfileImageOrDefault(binding.accountImageView)

          onPersonChanged()
        }
        accountImageView.setOnClickListener {
          AccountsAndSettingsDialogFragment.newInstance()
            .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
        }
      } else {
        binding.accountImageView.visibility = View.GONE
      }
      viewModel.personData.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            binding.fab.hide()
            loadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading -> {
            binding.fab.hide()
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            binding.fab.show()
            loadingView.hideAll()

            setup(personRef)

            if (!consumedArgs && savedInstanceState == null) {
              consumedArgs = true

              viewPager.post {
                when (args.screen as Screen) {
                  Screen.Posts -> {
                    viewPager.currentItem = 0
                  }
                  Screen.Comments -> {
                    viewPager.currentItem = 1
                  }
                  Screen.About -> {
                    viewPager.currentItem = 2
                  }
                }
              }
            }
          }
        }
      }
      binding.fab.setup(preferences)

      slidingPaneController = SlidingPaneController(
        fragment = this@PersonTabbedFragment,
        slidingPaneLayout = binding.slidingPaneLayout,
        childFragmentManager = childFragmentManager,
        viewModel = viewModel,
        globalLayoutMode = preferences.globalLayoutMode,
        lockPanes = true,
        retainClosedPosts = preferences.retainLastPost,
        emptyScreenText = getString(R.string.select_a_post_or_comment),
        fragmentContainerId = R.id.post_fragment_container,
      ).apply {
        onPageSelectedListener = { isOpen ->
        }
        init()
      }

      title.alpha = 0f
      isAnimatingTitleIn = false
      isAnimatingTitleOut = false

      body.visibility = View.GONE

      banner.transitionName = "banner_image"

      val actionBarHeight = context.getDimenFromAttribute(
        androidx.appcompat.R.attr.actionBarSize,
      )
      appBar.addOnOffsetChangedListener { _, verticalOffset ->
        if (!isBindingAvailable()) {
          return@addOnOffsetChangedListener
        }

        val percentCollapsed = -verticalOffset / collapsingToolbarLayout.height.toDouble()
        val absPixelsShowing = collapsingToolbarLayout.height + verticalOffset

        if (absPixelsShowing <= actionBarHeight) {
          if (!isAnimatingTitleIn) {
            isAnimatingTitleIn = true
            isAnimatingTitleOut = false
            title.animate().alpha(1f)
          }
        } else if (percentCollapsed < 0.66) {
          if (!isAnimatingTitleOut) {
            isAnimatingTitleOut = true
            isAnimatingTitleIn = false
            title.animate().alpha(0f)
          }
        }
      }

      installOnActionResultHandler(
        context = context,
        moreActionsHelper = moreActionsHelper,
        snackbarContainer = coordinatorLayout,
      )
    }

    binding.fab.setOnClickListener {
      showOverflowMenu()
    }

    viewLifecycleOwner.lifecycleScope.launch {
      userTagsManager.onChangedFlow.collect {
        onUserTagChanged()
      }
    }
  }

  private fun onPersonChanged() {
    val personRef = personRef

    if (personRef != null) {
      viewModel.fetchPersonIfNotDone(personRef)
    }

    setup(personRef)
  }

  private fun showOverflowMenu() {
    if (!isBindingAvailable()) return

    val context = requireContext()
    val personData = viewModel.personData.valueOrNull ?: return
    val person = personData.personView.person
    val personRef = person.toPersonRef()

    val bottomMenu = BottomMenu(requireContext()).apply {
      addItemWithIcon(
        id = R.id.sort,
        title = getString(R.string.sort_by),
        icon = R.drawable.baseline_sort_24,
      )
      addDivider()

      addItemWithIcon(
        id = R.id.message,
        title = R.string.send_message,
        icon = R.drawable.baseline_message_24,
      )

      addItemWithIcon(
        id = R.id.tag_user,
        title = getString(R.string.tag_user_format, personRef.name),
        icon = R.drawable.outline_sell_24,
      )

      if (moreActionsHelper.fullAccount?.isPersonBlocked(personRef) == true) {
        addItemWithIcon(
          id = R.id.unblock_user,
          title = context.getString(R.string.unblock_this_user_format, personRef.name),
          icon = R.drawable.baseline_person_24,
        )
      } else {
        addItemWithIcon(
          id = R.id.block_user,
          title = context.getString(R.string.block_this_user_format, personRef.name),
          icon = R.drawable.baseline_person_off_24,
        )
      }

      setOnMenuItemClickListener {
        when (it.id) {
          R.id.sort -> {
            SortTypeMenuHelper(
              requireContext(),
              this@PersonTabbedFragment,
              { viewModel.sortType },
              { viewModel.sortType = it },
            ).show()
          }
          R.id.block_user -> {
            moreActionsHelper.blockPerson(id = person.id, block = true)
          }
          R.id.unblock_user -> {
            moreActionsHelper.blockPerson(id = person.id, block = false)
          }
          R.id.message -> {
            AddOrEditCommentFragment.showMessageDialog(
              childFragmentManager,
              viewModel.instance,
              person.id,
            )
          }
          R.id.tag_user -> {
            AddOrEditUserTagDialogFragment.show(
              fragmentManager = childFragmentManager,
              person = person,
            )
          }
        }
      }
    }

    requireSummitActivity().showBottomMenu(bottomMenu)
  }

  override fun onResume() {
    super.onResume()

    val slidingPaneLayout = binding.slidingPaneLayout

    requireSummitActivity().apply {
      if (navBarController.useNavigationRail) {
        navBarController.showBottomNav()
      } else {
        if (slidingPaneLayout.isSwipeEnabled && slidingPaneLayout.isOpen) {
          // Post screen is open. Do not manipulate the nav bar. Let the post screen handle it.
        } else {
          if (!slidingPaneLayout.isOpen) {
            showNavBar()
          }
        }
      }
      setupForFragment<PersonTabbedFragment>(animate = false)
    }
  }

  private fun setup(personRef: PersonRef?) {
    if (!isBindingAvailable()) return

    if (personRef == null) {
      // this can happen if the user tapped on the profile page and is not signed in.
      binding.loadingView.showErrorText(
        getString(R.string.error_not_signed_in),
      )
      binding.profileIcon.visibility = View.GONE
      binding.collapsingToolbarContent.visibility = View.GONE
      binding.viewPager.visibility = View.GONE
      binding.tabLayoutContainer.visibility = View.GONE
      binding.fab.hide()

      viewModel.clearPersonData()
      return
    }

    val data = viewModel.personData.valueOrNull

    if (viewModel.personData.isLoading) {
      binding.loadingView.showProgressBar()
    }

    if (data == null) {
      binding.profileIcon.visibility = View.GONE
      binding.collapsingToolbarContent.visibility = View.GONE
      binding.viewPager.visibility = View.GONE
      binding.tabLayoutContainer.visibility = View.GONE
      binding.fab.hide()
      return
    }

    val person = data.personView.person
    val context = requireContext()

    Log.d(TAG, "user id: ${person.id}")
    Log.d(TAG, "actor id: ${person.actor_id}")

    TransitionManager.beginDelayedTransition(binding.collapsingToolbarContent)

    with(binding) {
      profileIcon.visibility = View.VISIBLE
      collapsingToolbarContent.visibility = View.VISIBLE
      viewPager.visibility = View.VISIBLE
      tabLayoutContainer.visibility = View.VISIBLE

      fab.show()

      val displayName = person.display_name
        ?: person.name

      title.text = displayName

      val bannerUrl = person.banner
      if (bannerUrl != null) {
        bannerDummy.setOnClickListener {
          getMainActivity()?.openImage(
            banner,
            null,
            personRef.fullName,
            bannerUrl,
            null,
          )
        }
        offlineManager.fetchImage(root, bannerUrl) {
          banner.load(it) {
            allowHardware(false)
          }
        }
      } else {
        banner.load("file:///android_asset/banner_placeholder.svg") {
          allowHardware(false)
        }
        bannerDummy.setOnClickListener(null)
      }
      avatarHelper.loadAvatar(profileIcon, person)
      val avatarUrl = person.avatar
      if (avatarUrl.isNullOrBlank()) {
        profileIcon.setOnClickListener {
          launchAlertDialog("error_user_has_no_profile_image") {
            messageResId = R.string.error_user_has_no_profile_image
          }
        }
      } else {
        ViewCompat.setTransitionName(profileIcon, "profileIcon")

        profileIcon.setOnClickListener {
          getMainActivity()?.openImage(
            sharedElement = profileIcon,
            appBar = null,
            title = person.fullName,
            url = avatarUrl,
            mimeType = null,
          )
        }
      }
      name.text = displayName

      val dateTime = LocalDateTime.ofEpochSecond(
        dateStringToTs(person.published) / 1000,
        0,
        ZoneOffset.UTC,
      )
      val now = LocalDateTime.now()
      val currentYear = now.year
      var nextBirthday = dateTime.withYear(currentYear)
      var daysBetween = now.until(nextBirthday, ChronoUnit.DAYS)

      if (daysBetween < 0) {
        nextBirthday = dateTime.withYear(currentYear + 1)
        daysBetween = now.until(nextBirthday, ChronoUnit.DAYS)
      }

      val dateStr = dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
      val cakeDrawable = context.getDrawableCompat(R.drawable.baseline_cake_24)
      val drawableSize = Utils.convertDpToPixel(13f).toInt()
      cakeDrawable?.setBounds(0, 0, drawableSize, drawableSize)
      cakeDate.setCompoundDrawablesRelative(
        cakeDrawable,
        null,
        null,
        null,
      )
      cakeDate.text =
        getString(R.string.cake_day_on_format, dateStr, defaultDecimalFormat.format(daysBetween))
      cakeDate.visibility = View.VISIBLE

      val personCreationTs = dateStringToTs(person.published)
      val isPersonNew =
        System.currentTimeMillis() - personCreationTs < NEW_PERSON_DURATION
      body.text = buildSpannedString {
        if (person.deleted) {
          val s = length
          append(context.getString(R.string.deleted_account))
          val e = length
          setSpan(
            RoundedBackgroundSpan(
              backgroundColor = context.getColorCompat(R.color.style_red),
              textColor = context.getColorCompat(R.color.white97),
            ),
            s,
            e,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
          )
          appendLine()
          appendLine()
        }
        if (isPersonNew) {
          val s = length
          append(
            context.getString(
              R.string.new_account_desc_format, tsToConcise(context, person.published),
            ),
          )
          val e = length
          setSpan(
            RoundedBackgroundSpan(
              backgroundColor = context.getColorCompat(R.color.style_amber),
              textColor = context.getColorCompat(R.color.black97),
            ),
            s,
            e,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
          )
          appendLine()
          appendLine()
        }

        append(
          context.resources.getQuantityString(
            R.plurals.posts_format,
            data.personView.counts.post_count,
            defaultDecimalFormat.format(data.personView.counts.post_count),
          ),
        )

        appendSeparator()

        append(
          context.resources.getQuantityString(
            R.plurals.comments_format,
            data.personView.counts.comment_count,
            defaultDecimalFormat.format(data.personView.counts.comment_count),
          ),
        )

        appendSeparator()

        append(
          context.getString(
            R.string.account_age_format,
            person.getAccountAgeString(),
          ),
        )
      }

      if (viewPager.adapter == null) {
        viewPager.offscreenPageLimit = 5
        val adapter =
          ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)
        adapter.addFrag(PersonPostsFragment::class.java, getString(R.string.posts))
        adapter.addFrag(PersonCommentsFragment::class.java, getString(R.string.comments))
        adapter.addFrag(PersonAboutFragment::class.java, getString(R.string.about))
        viewPager.adapter = adapter
      }

      tabLayoutContainer.visibility = View.VISIBLE
      TabLayoutMediator(
        tabLayout,
        binding.viewPager,
        binding.viewPager.adapter as ViewPagerAdapter,
      ).attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)

      if (body.text.isNullOrBlank()) {
        body.visibility = View.GONE
      } else {
        body.visibility = View.VISIBLE
      }
    }
    onUserTagChanged()
  }

  private fun onUserTagChanged() {
    val data = viewModel.personData.valueOrNull ?: return

    binding.subtitle.text = buildSpannedString {
      append(data.personView.person.fullName)

      val tag = userTagsManager.getUserTag(data.personView.person.fullName)
      if (tag != null) {
        append(" ")
        val s = length
        append(tag.tagName)
        val e = length

        setSpan(
          RoundedBackgroundSpan(tag.fillColor, tag.borderColor),
          s,
          e,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }
    }
  }

  fun closePost(postFragment: Fragment) {
    slidingPaneController?.closePost(postFragment)
  }

  override fun navigateToSignInScreen() {
    val direction = PostFragmentDirections.actionGlobalLogin()
    findNavController().navigateSafe(direction)
  }

  override fun proceedAnyways(tag: Int) {
  }
}
