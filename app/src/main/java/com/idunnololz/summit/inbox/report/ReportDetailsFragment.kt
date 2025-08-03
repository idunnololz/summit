package com.idunnololz.summit.inbox.report

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetailsResponse
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentReportDetailsBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.LemmyHeaderHelper.Companion.NEW_PERSON_DURATION
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.appendNameWithInstance
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.getAccountAgeString
import com.idunnololz.summit.inbox.InboxItem
import com.idunnololz.summit.inbox.InboxTabbedFragment
import com.idunnololz.summit.inbox.ReportItem
import com.idunnololz.summit.inbox.inbox.InboxViewModel
import com.idunnololz.summit.inbox.message.ContextFetcher
import com.idunnololz.summit.lemmy.mod.ModActionsDialogFragment
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.lemmy.post.PostAdapter
import com.idunnololz.summit.lemmy.post.PostViewModel
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.postAndCommentView.createCommentActionHandler
import com.idunnololz.summit.lemmy.postListView.showMorePostOptions
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.lemmy.userTags.UserTagsManager
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.showMoreVideoOptions
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.settings.misc.DisplayInstanceOptions
import com.idunnololz.summit.spans.RoundedBackgroundSpan
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.PrettyPrintStyles
import com.idunnololz.summit.util.PrettyPrintUtils.defaultDecimalFormat
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.shimmer.newShimmerDrawable16to9
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.tsToConcise
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class ReportDetailsFragment : BaseFragment<FragmentReportDetailsBinding>() {

  private val args: ReportDetailsFragmentArgs by navArgs()
  private val viewModel: ReportDetailsViewModel by viewModels()
  private val inboxViewModel: InboxViewModel by activityViewModels()

  @Inject
  lateinit var avatarHelper: AvatarHelper

  @Inject
  lateinit var userTagsManager: UserTagsManager

  @Inject
  lateinit var moreActionsHelper: MoreActionsHelper

  @Inject
  lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

  @Inject
  lateinit var accountActionsManager: AccountActionsManager

  @Inject
  lateinit var accountManager: AccountManager

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  @Inject
  lateinit var offlineManager: OfflineManager

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentReportDetailsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    val reportItem = args.reportItem

    if (savedInstanceState == null) {
      viewModel.isResolved.value = reportItem.isRead
    }

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, toolbar)
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, mainContainer)
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, fabContainer)
      }

      val title = when (reportItem) {
        is InboxItem.ReportCommentInboxItem -> {
          getString(R.string.report_on_comment)
        }
        is InboxItem.ReportMessageInboxItem -> {
          getString(R.string.report_on_private_message)
        }
        is InboxItem.ReportPostInboxItem -> {
          getString(R.string.report_on_post)
        }
      }

      setupToolbar(toolbar, title)
      toolbar.setNavigationOnClickListener {
        (parentFragment as? InboxTabbedFragment)?.closeMessage()
      }

      avatarHelper.loadAvatar(
        imageView = reporterIcon,
        person = reportItem.creator,
      )

      val reportCreatorInstance = reportItem.creator.instance
      val displayFullName = when (preferences.displayInstanceStyle) {
        DisplayInstanceOptions.NeverDisplayInstance -> {
          false
        }
        DisplayInstanceOptions.OnlyDisplayNonLocalInstances -> {
          viewModel.apiInstance != reportCreatorInstance
        }
        DisplayInstanceOptions.AlwaysDisplayInstance -> {
          true
        }
        else -> false
      }

      val sb = SpannableStringBuilder()
      if (displayFullName) {
        sb.appendNameWithInstance(
          context = context,
          name = if (preferences.preferUserDisplayName) {
            reportItem.creator.display_name
              ?: reportItem.creator.name
          } else {
            reportItem.creator.name
          },
          instance = reportCreatorInstance,
          url = LinkUtils.getLinkForPerson(reportItem.creator.toPersonRef()),
        )
      } else {
        sb.appendLink(
          if (preferences.preferUserDisplayName) {
            reportItem.creator.display_name
              ?: reportItem.creator.name
          } else {
            reportItem.creator.name
          },
          url = LinkUtils.getLinkForPerson(reportItem.creator.toPersonRef()),
        )
      }
      reporterName.text = sb
      reporterView.setOnClickListener {
        getMainActivity()?.launchPage(reportItem.creator.toPersonRef())
      }

      val resolver = reportItem.resolver
      if (resolver != null) {
        resolvedByView.visibility = View.VISIBLE
        resolvedBy.visibility = View.VISIBLE

        avatarHelper.loadAvatar(
          imageView = resolvedByIcon,
          person = resolver,
        )

        val reportResolverInstance = resolver.instance
        val displayFullName = when (preferences.displayInstanceStyle) {
          DisplayInstanceOptions.NeverDisplayInstance -> {
            false
          }
          DisplayInstanceOptions.OnlyDisplayNonLocalInstances -> {
            viewModel.apiInstance != reportResolverInstance
          }
          DisplayInstanceOptions.AlwaysDisplayInstance -> {
            true
          }
          else -> false
        }

        val sb = SpannableStringBuilder()
        if (displayFullName) {
          sb.appendNameWithInstance(
            context = context,
            name = if (preferences.preferUserDisplayName) {
              resolver.display_name
                ?: resolver.name
            } else {
              resolver.name
            },
            instance = reportResolverInstance,
            url = LinkUtils.getLinkForPerson(resolver.toPersonRef()),
          )
        } else {
          sb.appendLink(
            if (preferences.preferUserDisplayName) {
              resolver.display_name
                ?: resolver.name
            } else {
              resolver.name
            },
            url = LinkUtils.getLinkForPerson(resolver.toPersonRef()),
          )
        }
        resolvedByName.text = sb
        resolvedByView.setOnClickListener {
          getMainActivity()?.launchPage(resolver.toPersonRef())
        }
      } else {
        resolvedByView.visibility = View.GONE
        resolvedBy.visibility = View.GONE
      }

      content.text = reportItem.content

      fun goToPost() {
        when (reportItem) {
          is InboxItem.ReportCommentInboxItem -> {
            getMainActivity()?.launchPage(
              CommentRef(viewModel.apiInstance, reportItem.commentId),
            )
          }
          is InboxItem.ReportMessageInboxItem -> TODO()
          is InboxItem.ReportPostInboxItem -> {
            getMainActivity()?.launchPage(
              PostRef(viewModel.apiInstance, reportItem.reportedPostId),
            )
          }
        }
      }

      openContextButton.setOnClickListener {
        goToPost()
      }

      modActions.setOnClickListener {
        ModActionsDialogFragment.show(
          communityId = when (reportItem) {
            is InboxItem.ReportCommentInboxItem -> reportItem.communityId
            is InboxItem.ReportMessageInboxItem -> -1
            is InboxItem.ReportPostInboxItem -> reportItem.communityId
          },
          commentId = when (reportItem) {
            is InboxItem.ReportCommentInboxItem -> reportItem.reportedCommentId
            is InboxItem.ReportMessageInboxItem -> -1
            is InboxItem.ReportPostInboxItem -> -1
          },
          postId = when (reportItem) {
            is InboxItem.ReportCommentInboxItem -> -1
            is InboxItem.ReportMessageInboxItem -> -1
            is InboxItem.ReportPostInboxItem -> reportItem.reportedPostId
          },
          personId = reportItem.reportedPersonId,
          communityInstance = viewModel.apiInstance,
          fragmentManager = childFragmentManager,
        )
      }

      viewModel.commentContext.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            binding.contextLoadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading -> {
            binding.contextLoadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            binding.contextLoadingView.hideAll()
            loadRecyclerView(it.data)
          }
          null -> {}
        }
      }
      viewModel.reportedPersonInfo.observe(viewLifecycleOwner) {
        updateReportedPersonContext(it)
      }

      viewModel.fetchReportedPersonInfo(reportItem.reportedPersonId)
      viewModel.isResolved.observe(viewLifecycleOwner) {
        if (it) {
          fab.setImageResource(R.drawable.baseline_mark_as_unread_24)
          fab.setOnClickListener {
            inboxViewModel.markAsRead(args.reportItem, read = false)
          }
        } else {
          fab.setImageResource(R.drawable.baseline_check_24)
          fab.setOnClickListener {
            inboxViewModel.markAsRead(args.reportItem, read = true)
          }
        }
      }

      inboxViewModel.markAsReadResult.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error<*> -> {
            ErrorDialogFragment.show(
              context.getString(R.string.error_unable_to_resolve_user_report),
              it.error,
              childFragmentManager,
            )
          }
          is StatefulData.Loading<*> -> {}
          is StatefulData.NotStarted<*> -> {}
          is StatefulData.Success -> {
            viewModel.isResolved.value = it.data
          }
        }
      }

      when (reportItem) {
        is InboxItem.ReportMessageInboxItem -> {
          recyclerView.visibility = View.GONE
          openContextButton.visibility = View.GONE

          lemmyTextHelper.bindText(
            textView = contextText,
            text = reportItem.reportedContent,
            instance = viewModel.apiInstance,
            showMediaAsLinks = true,
            onImageClick = {
              getMainActivity()?.openImage(null, binding.appBar, null, it, null)
            },
            onVideoClick = { url ->
              getMainActivity()?.openVideo(url, VideoType.Unknown, null)
            },
            onPageClick = {
              getMainActivity()?.launchPage(it)
            },
            onLinkClick = { url, text, linkType ->
              onLinkClick(url, text, linkType)
            },
            onLinkLongClick = { url, text ->
              getSummitActivity()?.showMoreLinkOptions(url, text)
            },
          )
          contextInfo.text = tsToConcise(
            context = context,
            ts = dateStringToTs(reportItem.reportedContentLastUpdateTime),
            style = PrettyPrintStyles.SHORT_DYNAMIC,
          )
        }
        is InboxItem.ReportPostInboxItem,
        is InboxItem.ReportCommentInboxItem,
        -> {
          contextTextContainer.visibility = View.GONE
          loadContext(reportItem)
        }
      }
    }
  }

  private fun updateReportedPersonContext(data: StatefulData<GetPersonDetailsResponse>) {
    val context = binding.root.context
    val reportItem = args.reportItem

    binding.reportedPersonContext.apply {
      when (data) {
        is StatefulData.Error -> {
          contextLoadingView.showDefaultErrorMessageFor(data.error)
        }
        is StatefulData.Loading -> {
          contextLoadingView.showProgressBar()
        }
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          contextLoadingView.hideAll()

          val personView = data.data.person_view
          val person = data.data.person_view.person

          if (person.banner == null) {
            banner.visibility = View.GONE
            profileIcon.updateLayoutParams<ViewGroup.MarginLayoutParams> {
              topMargin = context.resources.getDimensionPixelSize(R.dimen.padding)
            }
          } else {
            banner.load(newShimmerDrawable16to9(context))
            offlineManager.fetchImageWithError(
              imageView = banner,
              url = person.banner,
              listener = {
                banner.load(it)
              },
              errorListener = {
                banner.visibility = View.GONE
              },
            )
            profileIcon.updateLayoutParams<ViewGroup.MarginLayoutParams> {
              topMargin = -context.resources.getDimensionPixelSize(R.dimen.padding)
            }
          }

          avatarHelper.loadAvatar(profileIcon, person)

          val displayName = person.display_name
            ?: person.name

          name.text = displayName
          subtitle.text = buildSpannedString {
            append(person.fullName)

            val tag = userTagsManager.getUserTag(person.fullName)
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

          val personCreationTs = dateStringToTs(person.published)
          val isPersonNew =
            System.currentTimeMillis() - personCreationTs < NEW_PERSON_DURATION

          body.text = if (person.bio.isNullOrBlank()) {
            context.getString(R.string.no_bio)
          } else {
            person.bio
          }

          underline.text = buildSpannedString {
            append(
              context.resources.getQuantityString(
                R.plurals.posts_format,
                personView.counts.post_count,
                defaultDecimalFormat.format(personView.counts.post_count),
              ),
            )

            appendSeparator()

            append(
              context.resources.getQuantityString(
                R.plurals.comments_format,
                personView.counts.comment_count,
                defaultDecimalFormat.format(personView.counts.comment_count),
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

          insights.text = buildSpannedString {
            var anyInsight = false
            fun addInsight(builderAction: SpannableStringBuilder.() -> Unit) {
              if (anyInsight) {
                appendLine()
                appendLine()
              }
              builderAction()

              anyInsight = true
            }

            if (person.deleted) {
              addInsight {
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
              }
            }

            if (person.banned) {
              addInsight {
                val s = length
                val banExpires = personView.person.ban_expires
                if (banExpires != null) {
                  append(
                    context.getString(
                      R.string.banned_until_format,
                      tsToConcise(context, banExpires),
                    ),
                  )
                } else {
                  append(context.getString(R.string.permanently_banned))
                }
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
              }
            }

            if (isPersonNew) {
              addInsight {
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
              }
            }

            if (person.bot_account) {
              addInsight {
                val s = length
                append(context.getString(R.string.bot_account))
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
              }
            }

            if (!anyInsight) {
              addInsight {
                val s = length
                append(context.getString(R.string.normal_account))
                val e = length
                setSpan(
                  RoundedBackgroundSpan(
                    backgroundColor = context.getColorCompat(R.color.style_blue),
                    textColor = context.getColorCompat(R.color.white97),
                  ),
                  s,
                  e,
                  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
              }
            }
          }
          modLogs.setOnClickListener {
            getMainActivity()?.launchModLogs(
              instance = viewModel.apiInstance,
              filterByMod = null,
              filterByUser = PersonRef.PersonRefById(
                reportItem.reportedPersonId,
                viewModel.apiInstance,
              ),
            )
          }

          openContextButton.setOnClickListener {
            getMainActivity()?.launchPage(
              PersonRef.PersonRefById(reportItem.reportedPersonId, viewModel.apiInstance),
            )
          }
        }
      }
    }
  }

  private fun loadContext(reportItem: ReportItem, force: Boolean = false) {
    when (val inboxItem = reportItem) {
      is InboxItem.ReportMessageInboxItem -> {
        Unit
      }
      is InboxItem.ReportCommentInboxItem -> {
        viewModel.fetchCommentContext(
          inboxItem.postId,
          inboxItem.reportedCommentPath,
          force,
        )
      }
      is InboxItem.ReportPostInboxItem -> {
        viewModel.fetchCommentContext(inboxItem.reportedPostId, null, force)
      }
    }
  }

  private fun loadRecyclerView(data: ContextFetcher.CommentContext) {
    if (!isBindingAvailable()) return

    val context = requireContext()

    with(binding) {
      var adapter = recyclerView.adapter as? PostAdapter

      if (adapter == null) {
        adapter = PostAdapter(
          postAndCommentViewBuilder = postAndCommentViewBuilder,
          context = context,
          lifecycleOwner = viewLifecycleOwner,
          instance = viewModel.apiInstance,
          accountId = null,
          revealAll = false,
          useFooter = false,
          isEmbedded = true,
          videoState = null,
          autoCollapseCommentThreshold = -1f,
          lemmyTextHelper = lemmyTextHelper,
          onRefreshClickCb = {
            loadContext(args.reportItem, force = true)
          },
          onSignInRequired = {
            PreAuthDialogFragment.newInstance()
              .show(childFragmentManager, "asdf")
          },
          onInstanceMismatch = { accountInstance, apiInstance ->
            OldAlertDialogFragment.Builder()
              .setTitle(R.string.error_account_instance_mismatch_title)
              .setMessage(
                getString(
                  R.string.error_account_instance_mismatch,
                  accountInstance,
                  apiInstance,
                ),
              )
              .createAndShow(childFragmentManager, "aa")
          },
          onAddCommentClick = { postOrComment ->
            if (viewModel.accountManager.currentAccount.value == null) {
              PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                .show(childFragmentManager, "asdf")
              return@PostAdapter
            }

            AddOrEditCommentFragment.showReplyDialog(
              instance = viewModel.apiInstance,
              postOrCommentView = postOrComment,
              fragmentManager = childFragmentManager,
              accountId = null,
            )
          },
          onImageClick = { _, view, url ->
            getMainActivity()?.openImage(view, binding.appBar, null, url, null)
          },
          onVideoClick = { url, videoType, state ->
            getMainActivity()?.openVideo(url, videoType, state)
          },
          onVideoLongClickListener = { url ->
            showMoreVideoOptions(
              url = url,
              originalUrl = url,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
            )
          },
          onPageClick = {
            getMainActivity()?.launchPage(it)
          },
          onPostActionClick = { postView, _, actionId ->
            showMorePostOptions(
              accountId = null,
              postView = postView,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
            )
          },
          onCommentActionClick = { commentView, _, actionId ->
            createCommentActionHandler(
              commentView = commentView,
              moreActionsHelper = moreActionsHelper,
              fragmentManager = childFragmentManager,
            )(actionId)
          },
          onFetchComments = {
            val postId = when (val inboxItem = args.reportItem) {
              is InboxItem.ReportMessageInboxItem -> TODO()
              is InboxItem.ReportCommentInboxItem -> inboxItem.postId
              is InboxItem.ReportPostInboxItem -> inboxItem.reportedPostId
            }

            getMainActivity()?.launchPage(
              PostRef(viewModel.apiInstance, postId),
            )
          },
          onLoadPost = { _, _ -> },
          onLoadCommentPath = {},
          onLinkClick = { url, text, linkType ->
            onLinkClick(url, text, linkType)
          },
          onLinkLongClick = { url, text ->
            getMainActivity()?.showMoreLinkOptions(url, text)
          },
          switchToNativeInstance = {},
          onCrossPostsClick = {},
        ).apply {
          stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        recyclerView.adapter = adapter
        recyclerView.doOnLayout {
          recyclerView.post {
            adapter.setContentMaxSize(
              recyclerView.measuredWidth,
              recyclerView.measuredHeight,
            )
          }
        }
        recyclerView.setup(animationsHelper)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
          OldThreadLinesDecoration(
            context,
            postAndCommentViewBuilder.hideCommentActions,
          ),
        )
      }

      adapter.setData(
        PostViewModel.PostData(
          postListView = PostViewModel.ListView.PostListView(
            post = data.post,
            postHeaderInfo = data.post.toPostHeaderInfo(context),
          ),
          commentTree = listOfNotNull(data.commentTree),
          newlyPostedCommentId = null,
          selectedCommentId = null,
          isSingleCommentChain = false,
          isNativePost = true,
          accountInstance = viewModel.apiInstance,
          isCommentsLoaded = true,
          commentPath = null,
          crossPosts = listOf(),
          wasUpdateForced = false,
        ),
      )

      val reportItem = args.reportItem

      adapter.highlightTintColor = context.getColorCompat(R.color.style_red)
      when (reportItem) {
        is InboxItem.ReportCommentInboxItem -> {
          val commentId = reportItem.commentId
          if (commentId != null) {
            adapter.highlightCommentForever(commentId)
          }
        }
        is InboxItem.ReportMessageInboxItem -> {
        }
        is InboxItem.ReportPostInboxItem -> {
          adapter.highlightPostForever = true
        }
      }
    }
  }
}
