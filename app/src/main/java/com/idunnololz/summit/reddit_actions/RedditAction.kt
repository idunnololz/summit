package com.idunnololz.summit.reddit_actions

import androidx.room.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

@Entity(tableName = "reddit_actions")
@TypeConverters(RedditActionConverters::class)
data class RedditAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val ts: Long,
    val creationTs: Long,
    val info: ActionInfo
)

sealed class ActionInfo(
    val action: ActionType,
    val isAffectedByRateLimit: Boolean,
    var retries: Int = 0
) {
    class UnknownActionInfo() : ActionInfo(ActionType.UNKNOWN, false)

    class VoteActionInfo(
        /**
         * Id of what to vote on
         */
        val id: String,
        /**
         * -1, 0 or 1
         */
        val dir: Int,
        val rank: Int
    ) : ActionInfo(ActionType.VOTE, true)

    class CommentActionInfo(
        /**
         * Id of what to comment on
         */
        val parentId: String,
        /**
         * The comment to post
         */
        val text: String
    ) : ActionInfo(ActionType.COMMENT, true)

    class DeleteCommentActionInfo(
        /**
         * Full id of the comment to delete
         */
        val id: String
    ) : ActionInfo(ActionType.COMMENT, true)

    class EditActionInfo(
        /**
         * Id of what to edit
         */
        val thingId: String,
        /**
         * The new text
         */
        val text: String
    ) : ActionInfo(ActionType.COMMENT, true)

}

private val gson by lazy {
    GsonBuilder()
        .registerTypeAdapter(ActionInfo::class.java, ActionInfoDeserializer())
        .create()
}

class ActionInfoDeserializer : JsonDeserializer<ActionInfo> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): ActionInfo? {

        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive
            if (primitive.isString && primitive.asString.isBlank()) {
                return null
            }
        }

        val jsonObject = json.asJsonObject
        val actionStr = jsonObject.get("action").asString
        val actionType = try {
            ActionType.valueOf(actionStr)
        } catch (e: IllegalArgumentException) {
            ActionType.UNKNOWN
        }

        return when (actionType) {
            ActionType.UNKNOWN -> {
                ActionInfo.UnknownActionInfo()
            }
            ActionType.VOTE -> {
                context.deserialize(json, ActionInfo.VoteActionInfo::class.java)
            }
            ActionType.COMMENT -> {
                context.deserialize(json, ActionInfo.CommentActionInfo::class.java)
            }
            ActionType.DELETE_COMMENT -> {
                context.deserialize(json, ActionInfo.DeleteCommentActionInfo::class.java)
            }
        }
    }

}

class RedditActionConverters {
    @TypeConverter
    fun actionInfoToString(value: ActionInfo): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun stringToActionInfo(value: String): ActionInfo = gson.fromJson(value, ActionInfo::class.java)
}