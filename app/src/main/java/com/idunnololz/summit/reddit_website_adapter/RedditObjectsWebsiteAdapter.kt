package com.idunnololz.summit.reddit_website_adapter

import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.reddit_objects.ListingObject
import com.idunnololz.summit.reddit_objects.RedditObject
import com.idunnololz.summit.scrape.GsonObjectWebsiteAdapter

class RedditObjectsWebsiteAdapter : GsonObjectWebsiteAdapter<List<RedditObject>>(
    listOf(), object : TypeToken<List<ListingObject>>() {}.type, false
)