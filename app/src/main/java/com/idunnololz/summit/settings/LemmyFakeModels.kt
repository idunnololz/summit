package com.idunnololz.summit.settings

import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentAggregates
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.api.dto.PostAggregates
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SubscribedType

object LemmyFakeModels {

    val fakeCommunity = Community(
        id = 0,
        name = "summit",
        title = "summit",
        description = null,
        removed = false,
        published = "2023-06-30T07:11:18Z",
        updated = null,
        deleted = false,
        nsfw = false,
        actor_id = "asdf",
        local = true,
        icon = null,
        banner = null,
        hidden = false,
        posting_restricted_to_mods = false,
        instance_id = 1,
    )

    val fakePerson1 = Person(
        id = 1,
        name = "idunnololz",
        display_name = "idunnololz",
        avatar = "null",
        banned = false,
        published = "2023-06-30T07:11:18Z",
        updated = null,
        actor_id = "http://meme.idunnololz.com",
        bio = null,
        local = false,
        banner = null,
        deleted = false,
        matrix_user_id = null,
        admin = false,
        bot_account = false,
        ban_expires = null,
        instance_id = 1,
    )

    val fakePerson2 = Person(
        id = 2,
        name = "rumias",
        display_name = "rumias",
        avatar = "null",
        banned = false,
        published = "2023-05-30T07:11:18Z",
        updated = null,
        actor_id = "http://meme.idunnololz.com",
        bio = null,
        local = false,
        banner = null,
        deleted = false,
        matrix_user_id = null,
        admin = false,
        bot_account = false,
        ban_expires = null,
        instance_id = 1,
    )

    val fakePost = Post(
        id = 10,
        name = "Example post",
        url = "https://lol-catalyst-data.s3.amazonaws.com/manual_uploads/sencha.jpg",
        body = "This is my cat Sencha. Isn't he cute? :D",
        creator_id = 1,
        community_id = 1,
        removed = false,
        locked = false,
        published = "2023-06-30T07:11:18Z",
        updated = null,
        deleted = false,
        nsfw = false,
        embed_title = null,
        embed_description = null,
        thumbnail_url = null,
        ap_id = "http://meme.idunnololz.com",
        local = true,
        embed_video_url = null,
        language_id = 1,
        featured_community = false,
        featured_local = false,
    )

    val fakePostView = PostView(
        post = fakePost,
        creator = fakePerson1,
        community = fakeCommunity,
        creator_banned_from_community = false,
        counts = PostAggregates(
            id = 1,
            post_id = 1,
            comments = 1,
            score = 420,
            upvotes = 489,
            downvotes = 69,
            published = "2023-06-30T07:11:18Z",
            newest_comment_time_necro = "2023-06-30T07:11:18Z",
            newest_comment_time = "2023-06-30T07:11:18Z",
            featured_community = false,
            featured_local = false,
        ),
        subscribed = SubscribedType.NotSubscribed,
        saved = true,
        read = true,
        creator_blocked = false,
        my_vote = null,
        unread_comments = 0,
    )

    val fakeCommentView1 = CommentView(
        comment = Comment(
            id = 1,
            creator_id = 1,
            post_id = 1,
            content = "This is a comment.",
            removed = false,
            published = "2023-04-30T07:11:18Z",
            updated = "2023-04-30T07:17:18Z",
            deleted = false,
            ap_id = "http://meme.idunnololz.com",
            local = true,
            path = "1234",
            distinguished = false,
            language_id = 1,
        ),
        creator = fakePerson2,
        post = fakePost,
        community = fakeCommunity,
        counts = CommentAggregates(
            1,
            1,
            123,
            142,
            19,
            published = "2023-04-30T07:12:18Z",
            1,
        ),
        creator_banned_from_community = false,
        subscribed = SubscribedType.NotSubscribed,
        saved = false,
        creator_blocked = false,
        my_vote = null,
    )

    val fakeCommentView2 = CommentView(
        comment = Comment(
            id = 2,
            creator_id = 2,
            post_id = 2,
            content = "Fascinating.",
            removed = false,
            published = "2023-04-30T07:11:18Z",
            updated = "2023-04-30T07:17:18Z",
            deleted = false,
            ap_id = "http://meme.idunnololz.com",
            local = true,
            path = "1234.456",
            distinguished = false,
            language_id = 1,
        ),
        creator = fakePerson1,
        post = fakePost,
        community = fakeCommunity,
        counts = CommentAggregates(
            1,
            1,
            3,
            3,
            0,
            published = "2023-04-30T07:13:18Z",
            0,
        ),
        creator_banned_from_community = false,
        subscribed = SubscribedType.NotSubscribed,
        saved = false,
        creator_blocked = false,
        my_vote = null,
    )

    val fakeCommentView3 = CommentView(
        comment = Comment(
            id = 3,
            creator_id = 1,
            post_id = 2,
            content = "Quite.",
            removed = false,
            published = "2023-04-30T07:11:18Z",
            updated = "2023-04-30T07:17:18Z",
            deleted = false,
            ap_id = "http://meme.idunnololz.com",
            local = true,
            path = "1234.456.789",
            distinguished = false,
            language_id = 1,
        ),
        creator = fakePerson2,
        post = fakePost,
        community = fakeCommunity,
        counts = CommentAggregates(
            3,
            1,
            4,
            5,
            1,
            published = "2023-04-31T07:13:18Z",
            0,
        ),
        creator_banned_from_community = false,
        subscribed = SubscribedType.NotSubscribed,
        saved = false,
        creator_blocked = false,
        my_vote = null,
    )
}
