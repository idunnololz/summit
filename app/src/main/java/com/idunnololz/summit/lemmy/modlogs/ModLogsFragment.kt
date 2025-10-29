package com.idunnololz.summit.lemmy.modlogs

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.RectF
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.ModlogActionType
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.CommunitiesLoadItemBinding
import com.idunnololz.summit.databinding.EmptyItemBinding
import com.idunnololz.summit.databinding.FragmentModLogsBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.ModEventItemBinding
import com.idunnololz.summit.databinding.ModLogsFilterItemBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.getName
import com.idunnololz.summit.lemmy.modlogs.ModLogsFragment.ModEventsAdapter.Item
import com.idunnololz.summit.lemmy.modlogs.filter.ModLogsFilterDialogFragment
import com.idunnololz.summit.lemmy.modlogs.modEvent.ModEventDialogFragment
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomLinkMovementMethod
import com.idunnololz.summit.util.DefaultLinkLongClickListener
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.durationToPretty
import com.idunnololz.summit.util.escapeMarkdown
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.showProgressBarIfNeeded
import com.idunnololz.summit.util.tsToConcise
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ModLogsFragment : BaseFragment<FragmentModLogsBinding>() {

  private val args by navArgs<ModLogsFragmentArgs>()

  private val viewModel: ModLogsViewModel by viewModels()

  @Inject
  lateinit var offlineManager: OfflineManager

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var linkResolver: LinkResolver

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentModLogsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      insetViewAutomaticallyByPaddingAndNavUi(
        viewLifecycleOwner,
        binding.rootView,
        applyTopInset = false,
      )
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

      setupToolbar(binding.toolbar, getString(R.string.mod_logs))
    }

    if (savedInstanceState == null) {
      viewModel.updateFilterWithByMod(args.filterByMod)
      viewModel.updateFilterWithByUser(args.filterByUser)
    }

    childFragmentManager.setFragmentResultListener(
      ModLogsFilterDialogFragment.REQUEST_KEY,
      viewLifecycleOwner,
    ) { k, bundle ->
      val result = bundle.getParcelableCompat<ModLogsFilterDialogFragment.Result>(
        ModLogsFilterDialogFragment.REQUEST_KEY_RESULT,
      )

      if (result != null) {
        viewModel.setFilter(result.filterConfig)
      }
    }

    viewModel.setArguments(args.instance, args.communityRef)
    if (viewModel.modLogData.isNotStarted) {
      viewModel.fetchModLogs(0)
    }

    runAfterLayout {
      setupView()
    }
  }

  private fun setupView() {
    val context = context ?: return

    fun showFilterDialog() {
      ModLogsFilterDialogFragment.show(childFragmentManager, viewModel.getFilter())
    }

    with(binding) {
      val adapter = ModEventsAdapter(
        context = context,
        instance = args.instance,
        availableWidth = binding.recyclerView.width,
        lemmyTextHelper = lemmyTextHelper,
        linkResolver = linkResolver,
        onPageClick = { url, pageRef ->
          getMainActivity()?.launchPage(pageRef, url = url)
        },
        onLoadPageClick = {
          viewModel.fetchModLogs(it)
        },
        onLinkClick = { url: String, text: String, linkContext: LinkContext ->
          onLinkClick(url, text, linkContext)
        },
        onLinkLongClick = { url: String, text: String ->
          getMainActivity()?.showMoreLinkOptions(url, text)
        },
        onFilterBannerClick = {
          showFilterDialog()
        },
        onModEventClick = {
          ModEventDialogFragment.show(childFragmentManager, it)
        },
      )
      val layoutManager = LinearLayoutManager(context)

      fun fetchPageIfLoadItem(position: Int) {
        (adapter.items.getOrNull(position) as? Item.LoadItem)
          ?.pageIndex
          ?.let {
            viewModel.fetchModLogs(it)
          }
      }

      fun checkIfFetchNeeded() {
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val lastPos = layoutManager.findLastVisibleItemPosition()

        for (i in (firstPos - 1)..(lastPos + 1)) {
          fetchPageIfLoadItem(i)
        }
      }

      recyclerView.setup(animationsHelper)
      recyclerView.setHasFixedSize(true)
      recyclerView.layoutManager = layoutManager
      recyclerView.adapter = adapter
      recyclerView.addOnScrollListener(
        object : RecyclerView.OnScrollListener() {
          override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            checkIfFetchNeeded()
          }
        },
      )
      fab.setup(preferences)
      fab.setOnClickListener {
        showFilterDialog()
      }

      swipeRefreshLayout.setOnRefreshListener {
        viewModel.reset()
      }

      fastScroller.setRecyclerView(recyclerView)

      viewModel.modLogData.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            swipeRefreshLayout.isRefreshing = false
            loadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading -> {
            loadingView.showProgressBarIfNeeded(swipeRefreshLayout)
          }
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            swipeRefreshLayout.isRefreshing = false
            loadingView.hideAll()

            if (it.data.resetScrollPosition) {
              layoutManager.scrollToPosition(0)
            }

            adapter.setItems(it.data.data)
          }
        }
      }

      viewModel.filter.observe(viewLifecycleOwner) {
        adapter.filter = it
      }
    }
  }

  override fun onResume() {
    super.onResume()

    setupForFragment<ModLogsFragment>()
  }

  private class ModEventsAdapter(
    private val context: Context,
    private val instance: String,
    private val availableWidth: Int,
    private val lemmyTextHelper: LemmyTextHelper,
    private val linkResolver: LinkResolver,
    private val onPageClick: (url: String, PageRef) -> Unit,
    private val onLoadPageClick: (Int) -> Unit,
    private val onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
    private val onLinkLongClick: (url: String, text: String) -> Unit,
    private val onFilterBannerClick: () -> Unit,
    private val onModEventClick: (modEvent: ModEvent) -> Unit,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed interface Item {
      data object HeaderItem : Item
      data class FilterItem(
        val filter: ModLogsFilterConfig,
      ) : Item
      data class ModEventItem(
        val modEvent: ModEvent,
      ) : Item
      data object FooterItem : Item

      data class LoadItem(
        val pageIndex: Int = 0,
      ) : Item
      data class ErrorItem(
        val pageIndex: Int,
        val error: Throwable,
      ) : Item
      data object EmptyItem : Item
    }

    var filter: ModLogsFilterConfig? = null
      set(value) {
        field = value

        refreshItems()
      }

    var data: List<ListEngine.Item<ModEvent>> = listOf()
      private set

    val items: List<Item>
      get() = adapterHelper.items

    private val summaryCharLength: Int

    init {
      val widthDp = Utils.convertPixelsToDp(availableWidth.toFloat())

      summaryCharLength = when {
        widthDp < 400f -> {
          60
        }
        widthDp < 600f -> {
          90
        }
        else -> {
          120
        }
      }
    }

    private val adapterHelper = AdapterHelper<Item>(
      areItemsTheSame = { old, new ->
        old::class == new::class &&
          when (old) {
            Item.EmptyItem -> true
            is Item.ErrorItem ->
              old.pageIndex == (new as Item.ErrorItem).pageIndex
            is Item.FilterItem -> true
            Item.FooterItem -> true
            Item.HeaderItem -> true
            is Item.LoadItem ->
              old.pageIndex == (new as Item.LoadItem).pageIndex
            is Item.ModEventItem ->
              old.modEvent.id == (new as Item.ModEventItem).modEvent.id
          }
      },
    ).apply {
      addItemType(
        clazz = Item.EmptyItem::class,
        inflateFn = EmptyItemBinding::inflate,
      ) { item, b, h ->
      }
      addItemType(
        clazz = Item.HeaderItem::class,
        inflateFn = ItemGenericHeaderBinding::inflate,
      ) { item, b, h ->
      }
      addItemType(
        clazz = Item.FooterItem::class,
        inflateFn = GenericSpaceFooterItemBinding::inflate,
      ) { item, b, h ->
      }
      addItemType(
        clazz = Item.ModEventItem::class,
        inflateFn = ModEventItemBinding::inflate,
      ) { item, b, h ->
        val modEvent = item.modEvent

        var description: String = modEvent::class.simpleName ?: ""
        var reason: String? = null

        when (modEvent) {
          is ModEvent.AdminPurgeCommentViewEvent -> {
            description = context.getString(
              R.string.purged_comment_format,
              "[${modEvent.event.post.name.summarize()}](${LinkUtils.getLinkForPost(
                instance,
                modEvent.event.post.id,
              )})",
            )
            reason = modEvent.event.admin_purge_comment.reason
          }
          is ModEvent.AdminPurgeCommunityViewEvent -> {
            description = context.getString(
              R.string.purged_community,
            )
            reason = modEvent.event.admin_purge_community.reason
          }
          is ModEvent.AdminPurgePersonViewEvent -> {
            description = context.getString(
              R.string.purged_person,
            )
            reason = modEvent.event.admin_purge_person.reason
          }
          is ModEvent.AdminPurgePostViewEvent -> {
            description = context.getString(
              R.string.purged_post_format,
              "[${modEvent.event.community.name}](${
                LinkUtils.getLinkForCommunity(
                  modEvent.event.community.instance,
                  modEvent.event.community.name,
                )})",
            )
            reason = modEvent.event.admin_purge_post.reason
          }
          is ModEvent.ModAddCommunityViewEvent -> {
            if (modEvent.event.mod_add_community.removed) {
              description = context.getString(
                R.string.removed_moderator_for_community_format,
                "[${modEvent.event.modded_person.name}](${LinkUtils.getLinkForPerson(
                  instance,
                  modEvent.event.modded_person.name,
                )})",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )})",
              )
            } else {
              description = context.getString(
                R.string.added_moderator_for_community_format,
                "[${modEvent.event.modded_person.name}](${LinkUtils.getLinkForPerson(
                  instance,
                  modEvent.event.modded_person.name,
                )})",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )})",
              )
            }
          }
          is ModEvent.ModAddViewEvent -> {
            if (modEvent.event.mod_add.removed) {
              description = context.getString(
                R.string.removed_moderator_for_site_format,
                "[${modEvent.event.modded_person.name}](${
                  LinkUtils.getLinkForPerson(
                    instance,
                    modEvent.event.modded_person.name,
                  )
                })",
                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
              )
            } else {
              description = context.getString(
                R.string.added_moderator_for_site_format,
                "[${modEvent.event.modded_person.name}](${
                  LinkUtils.getLinkForPerson(
                    instance,
                    modEvent.event.modded_person.name,
                  )
                })",
                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
              )
            }
          }
          is ModEvent.ModBanFromCommunityViewEvent -> {
            if (modEvent.event.mod_ban_from_community.banned) {
              val banExpires = modEvent.event.mod_ban_from_community.expires
              if (banExpires == null) {
                description = context.getString(
                  R.string.banned_person_from_community_permanently_format,
                  "[${modEvent.event.banned_person.name}](${
                    LinkUtils.getLinkForPerson(
                      instance,
                      modEvent.event.banned_person.name,
                    )
                  })",
                  "[${modEvent.event.community.name}](${
                    LinkUtils.getLinkForCommunity(
                      modEvent.event.community.instance,
                      modEvent.event.community.name,
                    )
                  })",
                )
              } else {
                val banDuration =
                  dateStringToTs(banExpires) -
                    dateStringToTs(modEvent.event.mod_ban_from_community.when_)

                description = context.getString(
                  R.string.banned_person_from_community_for_format,
                  "[${modEvent.event.banned_person.name}](${
                    LinkUtils.getLinkForPerson(
                      instance,
                      modEvent.event.banned_person.name,
                    )
                  })",
                  "[${modEvent.event.community.name}](${
                    LinkUtils.getLinkForCommunity(
                      modEvent.event.community.instance,
                      modEvent.event.community.name,
                    )
                  })",
                  durationToPretty(banDuration),
                )
              }
            } else {
              description = context.getString(
                R.string.unbanned_person_from_community_format,
                "[${modEvent.event.banned_person.name}](${
                  LinkUtils.getLinkForPerson(
                    instance,
                    modEvent.event.banned_person.name,
                  )
                })",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            }
            reason = modEvent.event.mod_ban_from_community.reason
          }
          is ModEvent.ModBanViewEvent -> {
            if (modEvent.event.mod_ban.banned) {
              val banExpires = modEvent.event.mod_ban.expires
              if (banExpires == null) {
                description = context.getString(
                  R.string.banned_person_from_site_permanently_format,
                  "[${modEvent.event.banned_person.name}](${
                    LinkUtils.getLinkForPerson(
                      instance,
                      modEvent.event.banned_person.name,
                    )
                  })",
                  "[$instance](${LinkUtils.getLinkForInstance(instance)})",
                )
              } else {
                val banDuration =
                  dateStringToTs(banExpires) -
                    dateStringToTs(modEvent.event.mod_ban.when_)

                description = context.getString(
                  R.string.banned_person_from_site_permanently_for_format,
                  "[${modEvent.event.banned_person.name}](${
                    LinkUtils.getLinkForPerson(
                      instance,
                      modEvent.event.banned_person.name,
                    )
                  })",
                  "[$instance](${LinkUtils.getLinkForInstance(instance)})",
                  durationToPretty(banDuration),
                )
              }
            } else {
              description = context.getString(
                R.string.unbanned_person_from_site_format,
                "[${modEvent.event.banned_person.name}](${
                  LinkUtils.getLinkForPerson(
                    instance,
                    modEvent.event.banned_person.name,
                  )
                })",
                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
              )
            }
            reason = modEvent.event.mod_ban.reason
          }
          is ModEvent.ModFeaturePostViewEvent -> {
            if (modEvent.event.mod_feature_post.featured) {
              description = context.getString(
                R.string.featured_post_in_community_format,
                "[${modEvent.event.post.name.summarize()}](${
                  LinkUtils.getLinkForPost(
                    instance,
                    modEvent.event.post.id,
                  )
                })",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            } else {
              description = context.getString(
                R.string.unfeatured_post_in_community_format,
                "[${modEvent.event.post.name.summarize()}](${
                  LinkUtils.getLinkForPost(
                    instance,
                    modEvent.event.post.id,
                  )
                })",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            }
          }
          is ModEvent.ModHideCommunityViewEvent -> {
            if (modEvent.event.mod_hide_community.hidden) {
              description = context.getString(
                R.string.hidden_community_format,
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            } else {
              description = context.getString(
                R.string.unhidden_community_format,
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            }
            reason = modEvent.event.mod_hide_community.reason
          }
          is ModEvent.ModLockPostViewEvent -> {
            if (modEvent.event.mod_lock_post.locked) {
              description = context.getString(
                R.string.locked_post_format,
                "[${modEvent.event.post.name.summarize()}](${
                  LinkUtils.getLinkForPost(
                    instance,
                    modEvent.event.post.id,
                  )
                })",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            } else {
              description = context.getString(
                R.string.unlocked_post_format,
                "[${modEvent.event.post.name.summarize()}](${
                  LinkUtils.getLinkForPost(
                    instance,
                    modEvent.event.post.id,
                  )
                })",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            }
          }
          is ModEvent.ModRemoveCommentViewEvent -> {
            if (modEvent.event.mod_remove_comment.removed) {
              description = context.getString(
                R.string.removed_comment_format,
                "[${modEvent.event.comment.content.summarize()}](${
                  LinkUtils.getLinkForComment(
                    instance,
                    modEvent.event.comment.id,
                  )
                })",
                "[${modEvent.event.post.name.summarize()}](${
                  LinkUtils.getLinkForPost(
                    instance,
                    modEvent.event.post.id,
                  )
                })",
              )
            } else {
              description = context.getString(
                R.string.unremoved_comment_format,
                "[${modEvent.event.comment.content.summarize()}](${
                  LinkUtils.getLinkForComment(
                    instance,
                    modEvent.event.comment.id,
                  )
                })",
                "[${modEvent.event.post.name.summarize()}](${
                  LinkUtils.getLinkForPost(
                    instance,
                    modEvent.event.post.id,
                  )
                })",
              )
            }
            reason = modEvent.event.mod_remove_comment.reason
          }
          is ModEvent.ModRemoveCommunityViewEvent -> {
            if (modEvent.event.mod_remove_community.removed) {
              description = context.getString(
                R.string.removed_community_format,
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
              )
            } else {
              description = context.getString(
                R.string.unremoved_community_format,
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
                "[$instance](${LinkUtils.getLinkForInstance(instance)})",
              )
            }
            reason = modEvent.event.mod_remove_community.reason
          }
          is ModEvent.ModRemovePostViewEvent -> {
            if (modEvent.event.mod_remove_post.removed) {
              description = context.getString(
                R.string.removed_post_format,
                "[${modEvent.event.post.name.summarize()}](${
                  LinkUtils.getLinkForPost(
                    instance,
                    modEvent.event.post.id,
                  )
                })",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            } else {
              description = context.getString(
                R.string.unremoved_post_format,
                "[${modEvent.event.post.name.summarize()}](${
                  LinkUtils.getLinkForPost(
                    instance,
                    modEvent.event.post.id,
                  )
                })",
                "[${modEvent.event.community.name}](${
                  LinkUtils.getLinkForCommunity(
                    modEvent.event.community.instance,
                    modEvent.event.community.name,
                  )
                })",
              )
            }
            reason = modEvent.event.mod_remove_post.reason
          }
          is ModEvent.ModTransferCommunityViewEvent -> {
            description = context.getString(
              R.string.transferred_ownership_of_community_format,
              "[${modEvent.event.community.name}](${
                LinkUtils.getLinkForCommunity(
                  modEvent.event.community.instance,
                  modEvent.event.community.name,
                )})",
              "[${modEvent.event.modded_person.name}](${LinkUtils.getLinkForPerson(
                instance,
                modEvent.event.modded_person.name,
              )})",
            )
          }
        }
        val modEventColor = modEvent.getColor(context)
        val containerColor = context.getColorFromAttribute(
          com.google.android.material.R.attr.colorSurfaceContainerHigh,
        )
        b.icon.setImageResource(modEvent.getIconRes())
        b.icon.imageTintList = ColorStateList.valueOf(modEventColor)
        b.icon.backgroundTintList = ColorStateList.valueOf(
          ColorUtils.blendARGB(
            modEventColor,
            containerColor,
            0.9f,
          ),
        )

        b.overtext.text = SpannableStringBuilder().apply {
          append(
            context.getString(
              R.string.mod_action_format,
              modEvent.actionOrder.toString(),
            ),
          )

          val agent = modEvent.agent
          if (agent != null) {
            appendSeparator()
            val s = length
            append(agent.name)
            val e = length
            setSpan(
              URLSpan(LinkUtils.getLinkForPerson(instance, agent.name)),
              s,
              e,
              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
          }
        }
        b.overtext.movementMethod = CustomLinkMovementMethod().apply {
          onLinkClickListener = object : CustomLinkMovementMethod.OnLinkClickListener {
            override fun onClick(
              textView: TextView,
              url: String,
              text: String,
              rect: RectF,
            ): Boolean {
              val pageRef = linkResolver.parseUrl(url, instance)

              if (pageRef != null) {
                onPageClick(url, pageRef)
              } else {
                onLinkClick(url, text, LinkContext.Text)
              }
              return true
            }
          }
          onLinkLongClickListener = DefaultLinkLongClickListener(
            b.root.context,
            onLinkLongClick,
          )
        }
        b.date.text = tsToConcise(context, modEvent.ts)
        lemmyTextHelper.bindText(
          textView = b.title,
          text = description,
          instance = instance,
          onImageClick = {},
          onVideoClick = {},
          onPageClick = { url, pageRef ->
            onPageClick(url, pageRef)
          },
          onLinkClick = { url, text, linkType ->
            onLinkClick(url, text, linkType)
          },
          onLinkLongClick = { url, text ->
            onLinkLongClick(url, text)
          },
        )
        if (reason == null) {
          b.body.visibility = View.GONE
        } else {
          b.body.visibility = View.VISIBLE
          b.body.text = context.getString(R.string.reason_format, reason)
        }

        b.root.setOnClickListener {
          onModEventClick(item.modEvent)
        }
      }
      addItemType(
        clazz = Item.LoadItem::class,
        inflateFn = CommunitiesLoadItemBinding::inflate,
      ) { item, b, h ->
        b.loadingView.showProgressBar()
      }
      addItemType(
        clazz = Item.ErrorItem::class,
        inflateFn = CommunitiesLoadItemBinding::inflate,
      ) { item, b, h ->
        b.loadingView.showDefaultErrorMessageFor(item.error)
        b.loadingView.setOnRefreshClickListener {
          onLoadPageClick(item.pageIndex)
        }
      }
      addItemType(
        clazz = Item.FilterItem::class,
        inflateFn = ModLogsFilterItemBinding::inflate,
      ) { item, b, h ->
        val filterNames = mutableListOf<String>()

        if (item.filter.filterByActionType != ModlogActionType.All) {
          filterNames += item.filter.filterByActionType.getName(context)
        }
        if (item.filter.filterByMod != null) {
          filterNames += context.getString(R.string.mod_format, item.filter.filterByMod.name)
        }
        if (item.filter.filterByPerson != null) {
          filterNames += context.getString(R.string.user_format, item.filter.filterByPerson.name)
        }

        b.text.text = context.getString(R.string.filtering_by_format, filterNames.joinToString())
        b.root.setOnClickListener {
          onFilterBannerClick()
        }
      }
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    fun setItems(newItems: List<ListEngine.Item<ModEvent>>) {
      data = newItems

      refreshItems()
    }

    private fun refreshItems() {
      val data = data
      val filter = filter
      val newItems = mutableListOf<Item>()

      newItems += Item.HeaderItem

      if (filter?.isFilterDefault() == false) {
        newItems += Item.FilterItem(filter)
      }

      data.mapTo(newItems) {
        when (it) {
          is ListEngine.Item.DataItem<ModEvent> ->
            Item.ModEventItem(it.data)
          is ListEngine.Item.EmptyItem<*> ->
            Item.EmptyItem
          is ListEngine.Item.ErrorItem<*> ->
            Item.ErrorItem(it.pageIndex, it.error)
          is ListEngine.Item.LoadItem<*> ->
            Item.LoadItem(it.pageIndex)
        }
      }

      newItems += Item.FooterItem

      adapterHelper.setItems(newItems, this)
    }

    private fun String.summarize() = if (this.length > summaryCharLength + 3) {
      this.take(summaryCharLength) + "…"
    } else {
      this
    }.escapeMarkdown()
  }
}
