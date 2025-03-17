package com.idunnololz.summit.settings.backupAndRestore.export

enum class ExportableTable(val tableName: String) {
    UserCommunities("user_communities"),
    HiddenPosts("hidden_posts"),
    ContentFilters("content_filters"),
    Drafts("drafts"),
    TextEmojis("text_emojis"),
    UserTags("user_tags"),
}

val SYSTEM_TABLES = setOf(
    "android_metadata",
    "sqlite_sequence",
    "room_master_table",
)

fun tableNameToExportableTable(tableName: String) =
    ExportableTable.entries.firstOrNull { it.tableName == tableName }

val defaultTablesToExport: Set<String>
    get() {
        val tableNames = mutableSetOf<String>()
        tableNames.addAll(SYSTEM_TABLES)
        ExportableTable.entries.forEach {
            tableNames.add(it.tableName)
        }
        return tableNames
    }
