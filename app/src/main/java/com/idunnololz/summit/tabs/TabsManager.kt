package com.idunnololz.summit.tabs

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.Utils
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okio.buffer
import okio.sink
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class TabsManager(
    private val context: Context
) {
    companion object {
        const val FIRST_FRAGMENT_TAB_ID: String = "0"
        const val FIRST_FRAGMENT_SORT_ID: Long = 0

        lateinit var instance: TabsManager

        fun initialize(context: Context) {
            instance = TabsManager(context)
        }
    }

    var isLoading: Boolean = true
        private set

    private val tabPreviewsDir = File(context.filesDir, "tab_previews")

    private val tabsMap = HashMap<String, TabItem>()
    private val allTabs = arrayListOf<TabItem>()

    private val scheduler: Scheduler = Schedulers.from(AsyncTask.SERIAL_EXECUTOR)

    private var nextAvailableSortId: Long = 1

    val tabsChangedLiveData = MutableLiveData(this)
    val currentTabId = MutableLiveData(FIRST_FRAGMENT_TAB_ID)

    init {
        addNewTabOrUpdateInternal(makeHomeTab())

        @Suppress("CheckResult")
        MainDatabase.getInstance(context).tabsDao().getAllTabs()
            .map { tabEntry ->
                listOf(makeHomeTab()) + tabEntry.mapNotNull { it.toTabItem() }
            }
            .subscribeOn(scheduler)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ tabItem ->
                clearTabsInternal()

                tabItem.forEach {
                    addNewTabOrUpdateInternal(it)
                }

                nextAvailableSortId = checkNotNull(allTabs.maxByOrNull { it.sortId }).sortId + 1

                isLoading = false
                tabsChangedLiveData.value = this
            }, {})
    }

    private fun addNewTabOrUpdateInternal(newTab: TabItem?) {
        newTab ?: return

        tabsMap[newTab.tabId] = newTab
        val oldTabIndex = allTabs.indexOfFirst { it.tabId == newTab.tabId }
        if (oldTabIndex == -1) {
            allTabs.add(newTab)
        } else {
            allTabs[oldTabIndex] = newTab
        }
    }

    private fun removeTabInternal(tab: TabItem?) {
        tab ?: return

        tabsMap.remove(tab.tabId)
        allTabs.remove(tab)

        if (currentTabId.value == tab.tabId) {
            currentTabId.value = FIRST_FRAGMENT_TAB_ID
        }
    }

    private fun clearTabsInternal() {
        tabsMap.clear()
        allTabs.clear()
    }

    private fun addNewTabToDb(newTab: TabItem?) {
        newTab ?: return

        MainDatabase.getInstance(context)
            .tabsDao()
            .insertTab(newTab.toTabEntry())
            .subscribeOn(scheduler)
            .subscribe()
    }

    private fun removeTabFromDb(tab: TabItem?) {
        tab ?: return

        MainDatabase.getInstance(context)
            .tabsDao()
            .delete(tab.toTabEntry())
            .subscribeOn(scheduler)
            .subscribe()
    }

    private fun makeHomeTab(): TabItem =
        TabItem.PageTabItem(
            FIRST_FRAGMENT_TAB_ID,
            FIRST_FRAGMENT_SORT_ID,
            getThumbnailFileForTab(FIRST_FRAGMENT_TAB_ID).let { if (it.exists()) it.path else null },
            PreferenceUtil.preferences.getString(
                PreferenceUtil.KEY_DEFAULT_PAGE_THUMBNAIL_SIGNATURE,
                null
            ),
            PageDetails(
                PreferenceUtil.getDefaultPage(),
                false,
                null
            )
        )

    private fun getThumbnailFileForTab(tabId: String): File = File(tabPreviewsDir, "$tabId.png")

    fun getTab(tabId: String): TabItem? = tabsMap[tabId]

    fun addNewTab(tabItem: TabItem.PageTabItem): String {
        // 0 is reserved for HOME
        val sortId = nextAvailableSortId++
        val tabId = if (tabItem.tabId.isBlank()) {
            UUID.randomUUID().toString()
        } else {
            tabItem.tabId
        }

        val newTab = tabItem.copy(tabId = tabId, sortId = sortId)

        addNewTabOrUpdateInternal(newTab)
        addNewTabToDb(newTab)

        tabsChangedLiveData.value = this

        return tabId
    }

    fun closeTab(tabId: String) {
        val tab = tabsMap[tabId] ?: return

        removeTabInternal(tab)
        removeTabFromDb(tab)

        tabsChangedLiveData.value = this
    }

    fun updateTabPreview(tabId: String, bitmap: Bitmap) {
        val tabItem = checkNotNull(tabsMap[tabId])
        val previewDest = getThumbnailFileForTab(tabId)

        Single
            .fromCallable {
                tabPreviewsDir.mkdirs()

                previewDest.sink().buffer().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it.outputStream())
                }

                Utils.hashSha256("${previewDest.length()}:${System.currentTimeMillis()}")
            }
            .subscribeOn(scheduler)
            .flatMap { sig ->
                Single
                    .fromCallable {
                        tabItem.previewPath = previewDest.path
                        tabItem.previewSignature = sig

                        tabsChangedLiveData.value = this@TabsManager

                        sig
                    }
                    .subscribeOn(AndroidSchedulers.mainThread())
            }
            .flatMap { sig ->
                if (tabItem.tabId != FIRST_FRAGMENT_TAB_ID) {
                    MainDatabase.getInstance(context)
                        .tabsDao()
                        .insertTab(tabItem.toTabEntry())
                        .subscribeOn(scheduler)
                } else {
                    PreferenceUtil.preferences.edit()
                        .putString(PreferenceUtil.KEY_DEFAULT_PAGE_THUMBNAIL_SIGNATURE, sig)
                        .apply()
                    Single.just(0)
                }
            }
            .subscribe()
    }

    fun getTabsCount(): Int = allTabs.size

    fun getAllTabs(): List<TabItem> = allTabs

    fun onDefaultPageChanged() {
        addNewTabOrUpdateInternal(makeHomeTab())
    }
}