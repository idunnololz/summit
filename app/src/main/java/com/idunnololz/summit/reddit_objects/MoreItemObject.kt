package com.idunnololz.summit.reddit_objects

class MoreItemObject(
    kind: String,
    val data: MoreItem?
) : RedditObject(kind)

class MoreItem(
    val count: Int,
    val name: String,
    val id: String,
    val parentId: String,
    val depth: Int,
    val children: List<String>
)

/*
{
    "kind": "more",
    "data": {
        "count": 2,
        "name": "t1_fh2btt4",
        "id": "fh2btt4",
        "parent_id": "t1_fh2241t",
        "depth": 9,
        "children": ["fh2btt4", "fh326ke"]
    }
}
 */