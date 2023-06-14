package com.idunnololz.summit.reddit_objects

import com.google.gson.*
import java.lang.RuntimeException
import java.lang.reflect.Type

class RedditObjectTypeAdapter : JsonDeserializer<RedditObject>, JsonSerializer<RedditObject> {

    override fun serialize(
        src: RedditObject?,
        typeOfSrc: Type?,
        context: JsonSerializationContext
    ): JsonElement = context.serialize(src)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): RedditObject? {

        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive
            if (primitive.isString && primitive.asString.isBlank()) {
                return null
            }
        }

        val jsonObject = json.asJsonObject
        val kind = jsonObject.get("kind").asString

        return if (kind == "Listing") {
            context.deserialize<ListingObject>(json, ListingObject::class.java)
        } else if (kind == "t3") {
            context.deserialize<ListingItemObject>(json, ListingItemObject::class.java)
        } else if (kind == "t1") {
            context.deserialize<CommentItemObject>(json, CommentItemObject::class.java)
        } else if (kind == "t5") {
            context.deserialize<SubredditObject>(json, SubredditObject::class.java)
        } else if (kind == "more") {
            context.deserialize<MoreItemObject>(json, MoreItemObject::class.java)
        } else {
            throw RuntimeException("Unknown kind: $kind")
        }
    }
}