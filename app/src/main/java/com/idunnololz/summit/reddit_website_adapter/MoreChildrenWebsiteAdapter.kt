package com.idunnololz.summit.reddit_website_adapter

import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.reddit_objects.MoreChildrenObject
import com.idunnololz.summit.scrape.GsonObjectWebsiteAdapter

class MoreChildrenWebsiteAdapter : GsonObjectWebsiteAdapter<MoreChildrenObject>(
    MoreChildrenObject(), object : TypeToken<MoreChildrenObject>() {}.type, false
)