package com.idunnololz.summit.reddit_website_adapter

import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.reddit_objects.ListingObject
import com.idunnololz.summit.scrape.GsonObjectWebsiteAdapter

class RedditListingWebsiteAdapter : GsonObjectWebsiteAdapter<ListingObject>(
    ListingObject(), object : TypeToken<ListingObject>() {}.type, false
)