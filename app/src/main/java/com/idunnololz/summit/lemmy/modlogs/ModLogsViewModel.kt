package com.idunnololz.summit.lemmy.modlogs

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetModlogResponse
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.lemmy.inbox.repository.MultiLemmyListSource
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.requireMainThread
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

@HiltViewModel
class ModLogsViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
  private val noAuthApiClient: LemmyApiClient,
  private val accountManager: AccountManager,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  companion object {

    private const val TAG = "ModLogsViewModel"
  }

  private val apiCallConsolidator = ApiCallConsolidator(apiClient)

  private var communityRef: CommunityRef? = null
  private var communityView: CommunityView? = null

  val apiInstance: String
    get() = apiClient.instance
  private val modLogEngine = ListEngine<ModEvent>()

  val modLogData = StatefulLiveData<ModLogData>()

  private var modSource: MultiLemmyListSource<ModEvent, Unit>? = null

  var resetScrollPosition: Boolean = false

  private val pagesFetching = mutableSetOf<Int>()

  val filter = savedStateHandle.getLiveData("filter", ModLogsFilterConfig())

  init {
    filter.value?.let {
      apiCallConsolidator.filter = it
    }

    viewModelScope.launch {
      modLogEngine.items.collect {
        modLogData.postValue(ModLogData(it, resetScrollPosition = resetScrollPosition))
      }
    }
  }

  fun fetchModLogs(pageIndex: Int, force: Boolean = false, resetScrollPosition: Boolean = false) {
    requireMainThread()

    if (pagesFetching.contains(pageIndex)) {
      return
    }

    Log.d(TAG, "fetchModLogs(): $pageIndex, $force")
    modLogData.setIsLoading()
    pagesFetching.add(pageIndex)

    val communityRef = communityRef
    val communityView = communityView
    val account = accountManager.currentAccount.asAccount

    this.resetScrollPosition = resetScrollPosition

    val job = viewModelScope.launch {
      val communityIdOrNull: Result<CommunityId?> =
        if (communityRef is CommunityRef.CommunityRefByName) {
          if (communityView != null && communityView.community.name == communityRef.name) {
            Result.success(communityView.community.id)
          } else {
            noAuthApiClient.getCommunity(
              account = if (account?.instance == apiInstance) {
                account
              } else {
                null
              },
              idOrName = Either.Right(communityRef.getServerId(apiClient.instance)),
              force = force,
            ).fold(
              {
                this@ModLogsViewModel.communityView = it.community_view

                Result.success(it.community_view.community.id)
              },
              {
                Result.failure(it)
              },
            )
          }
        } else {
          Result.success(null)
        }

      if (communityIdOrNull.isFailure) {
        modLogData.postError(communityIdOrNull.exceptionOrNull()!!)
        return@launch
      }

      val communityId = communityIdOrNull.getOrThrow()

      val modSource = modSource ?: MultiLemmyListSource<ModEvent, Unit>(
        listOf(
          newSource(communityId, ModEvent.ModRemovePostViewEvent::class) {
            it.removed_posts.map {
              ModEvent.ModRemovePostViewEvent(
                it.mod_remove_post.id,
                ActionType.Mod,
                dateStringToTs(it.mod_remove_post.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModLockPostViewEvent::class) {
            it.locked_posts.map {
              ModEvent.ModLockPostViewEvent(
                it.mod_lock_post.id,
                ActionType.Mod,
                dateStringToTs(it.mod_lock_post.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModFeaturePostViewEvent::class) {
            it.featured_posts.map {
              ModEvent.ModFeaturePostViewEvent(
                it.mod_feature_post.id,
                ActionType.Mod,
                dateStringToTs(it.mod_feature_post.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModRemoveCommentViewEvent::class) {
            it.removed_comments.map {
              ModEvent.ModRemoveCommentViewEvent(
                it.mod_remove_comment.id,
                ActionType.Mod,
                dateStringToTs(it.mod_remove_comment.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModRemoveCommunityViewEvent::class) {
            it.removed_communities.map {
              ModEvent.ModRemoveCommunityViewEvent(
                it.mod_remove_community.id,
                ActionType.Mod,
                dateStringToTs(it.mod_remove_community.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModBanFromCommunityViewEvent::class) {
            it.banned_from_community.map {
              ModEvent.ModBanFromCommunityViewEvent(
                it.mod_ban_from_community.id,
                ActionType.Mod,
                dateStringToTs(it.mod_ban_from_community.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModBanViewEvent::class) {
            it.banned.map {
              ModEvent.ModBanViewEvent(
                it.mod_ban.id,
                ActionType.Mod,
                dateStringToTs(it.mod_ban.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModAddCommunityViewEvent::class) {
            it.added_to_community.map {
              ModEvent.ModAddCommunityViewEvent(
                it.mod_add_community.id,
                ActionType.Mod,
                dateStringToTs(it.mod_add_community.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModTransferCommunityViewEvent::class) {
            it.transferred_to_community.map {
              ModEvent.ModTransferCommunityViewEvent(
                it.mod_transfer_community.id,
                ActionType.Mod,
                dateStringToTs(it.mod_transfer_community.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModAddViewEvent::class) {
            it.added.map {
              ModEvent.ModAddViewEvent(
                it.mod_add.id,
                ActionType.Mod,
                dateStringToTs(it.mod_add.when_),
                it.moderator,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.AdminPurgePersonViewEvent::class) {
            it.admin_purged_persons.map {
              ModEvent.AdminPurgePersonViewEvent(
                it.admin_purge_person.id,
                ActionType.Admin,
                dateStringToTs(it.admin_purge_person.when_),
                it.admin,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.AdminPurgeCommunityViewEvent::class) {
            it.admin_purged_communities.map {
              ModEvent.AdminPurgeCommunityViewEvent(
                it.admin_purge_community.id,
                ActionType.Admin,
                dateStringToTs(it.admin_purge_community.when_),
                it.admin,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.AdminPurgePostViewEvent::class) {
            it.admin_purged_posts.map {
              ModEvent.AdminPurgePostViewEvent(
                it.admin_purge_post.id,
                ActionType.Admin,
                dateStringToTs(it.admin_purge_post.when_),
                it.admin,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.AdminPurgeCommentViewEvent::class) {
            it.admin_purged_comments.map {
              ModEvent.AdminPurgeCommentViewEvent(
                it.admin_purge_comment.id,
                ActionType.Admin,
                dateStringToTs(it.admin_purge_comment.when_),
                it.admin,
                it,
              )
            }
          },
          newSource(communityId, ModEvent.ModHideCommunityViewEvent::class) {
            it.hidden_communities.map {
              ModEvent.ModHideCommunityViewEvent(
                it.mod_hide_community.id,
                ActionType.Mod,
                dateStringToTs(it.mod_hide_community.when_),
                it.admin,
                it,
              )
            }
          },
        ),
        sortValue = { it.ts },
        id = { it.id.toLong() },
      )

      val result = modSource.getPage(pageIndex, force, true)

      val modEvents = result.fold(
        onSuccess = {
          it.items.sortedByDescending { it.ts }
        },
        onFailure = {
          null
        },
      )

      modLogEngine.addPage(
        page = pageIndex,
        communities = result.fold(
          onSuccess = {
            Result.success(modEvents!!)
          },
          onFailure = {
            Result.failure(it)
          },
        ),
        hasMore = result.fold(
          onSuccess = {
            it.items.isNotEmpty()
          },
          onFailure = {
            true
          },
        ),
      )
    }

    viewModelScope.launch {
      job.join()
      pagesFetching.remove(pageIndex)
    }
  }

  fun updateFilterWithByMod(personRef: PersonRef?) {
    personRef ?: return

    modLogData.setIsLoading()

    when (personRef) {
      is PersonRef.PersonRefById -> {
        viewModelScope.launch {
          apiClient.fetchPersonByIdWithRetry(
            personRef.id,
            force = false,
          ).onSuccess {
            setFilter(getFilter().copy(filterByMod = it.person_view.person.toPersonRef()))
          }.onFailure {
            modLogData.setError(it)
          }
        }
      }
      is PersonRef.PersonRefByName -> {
        viewModelScope.launch {
          apiClient.fetchPersonByNameWithRetry(
            personRef.fullName,
            force = false,
          ).onSuccess {
            setFilter(getFilter().copy(filterByMod = it.person_view.person.toPersonRef()))
          }.onFailure {
            modLogData.setError(it)
          }
        }
      }
      is PersonRef.PersonRefComplete -> {
        setFilter(getFilter().copy(filterByMod = personRef))
      }
    }
  }

  fun updateFilterWithByUser(personRef: PersonRef?) {
    personRef ?: return

    modLogData.setIsLoading()

    when (personRef) {
      is PersonRef.PersonRefById -> {
        viewModelScope.launch {
          apiClient.fetchPersonByIdWithRetry(
            personRef.id,
            force = false,
          ).onSuccess {
            setFilter(getFilter().copy(filterByPerson = it.person_view.person.toPersonRef()))
          }.onFailure {
            modLogData.setError(it)
          }
        }
      }
      is PersonRef.PersonRefByName -> {
        viewModelScope.launch {
          apiClient.fetchPersonByNameWithRetry(
            personRef.fullName,
            force = false,
          ).onSuccess {
            setFilter(getFilter().copy(filterByPerson = it.person_view.person.toPersonRef()))
          }.onFailure {
            modLogData.setError(it)
          }
        }
      }
      is PersonRef.PersonRefComplete -> {
        setFilter(getFilter().copy(filterByPerson = personRef))
      }
    }
  }

  fun newSource(
    communityId: CommunityId?,
    clazz: KClass<*>,
    transformResult: (GetModlogResponse) -> List<ModEvent>,
  ) = LemmyListSource<ModEvent, Unit>(
    context = context,
    id = { this.id.toLong() },
    defaultSortOrder = Unit,
    fetchObjects = { page: Int, sortOrder: Unit, limit: Int, force: Boolean ->
      apiCallConsolidator.fetchObjects(communityId, page, limit, force)
        .fold(
          {
            Result.success(transformResult(it).sortedByDescending { it.ts })
          },
          {
            Result.failure(it)
          },
        )
    },
    pageSize = 10,
    source = clazz,
  )

  fun setArguments(instance: String, communityRef: CommunityRef?) {
    noAuthApiClient.changeInstance(instance)
    this.communityRef = communityRef
  }

  fun setFilter(config: ModLogsFilterConfig) {
    filter.value = config
    reset()
  }

  fun getFilter(): ModLogsFilterConfig {
    return filter.value ?: ModLogsFilterConfig()
  }

  fun reset() {
    pagesFetching.clear()
    apiCallConsolidator.clear()
    apiCallConsolidator.filter = getFilter()

    viewModelScope.launch {
      modLogData.clear()
      fetchModLogs(0, force = true, resetScrollPosition = true)
    }
  }

  class ApiCallConsolidator(
    private val apiClient: AccountAwareLemmyClient,
  ) {

    class CacheData(
      val data: Result<GetModlogResponse>,
      val ts: Long,
    )

    val cache = mutableMapOf<String, CacheData>()
    var filter = ModLogsFilterConfig()

    suspend fun fetchObjects(
      communityId: CommunityId?,
      page: Int,
      limit: Int,
      force: Boolean,
    ): Result<GetModlogResponse> {
      val key = "$communityId;$page;$limit;$force"
      val cachedValue = cache[key]

      if (cachedValue != null && 5_000 > System.currentTimeMillis() - cachedValue.ts) {
        return cachedValue.data
      }

      val result = apiClient.fetchModLogs(
        modPersonId = filter.filterByMod?.id,
        communityId = communityId,
        page = page,
        limit = limit,
        actionType = filter.filterByActionType,
        otherPersonId = filter.filterByPerson?.id,
        force = force,
      )

      cache[key] = CacheData(
        result,
        System.currentTimeMillis(),
      )

      return result
    }

    fun clear() {
      cache.clear()
    }
  }

  data class ModLogData(
    val data: List<ListEngine.Item<ModEvent>>,
    val resetScrollPosition: Boolean,
  )
}
