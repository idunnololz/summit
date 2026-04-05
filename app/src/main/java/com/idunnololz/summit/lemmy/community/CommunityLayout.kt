package com.idunnololz.summit.lemmy.community

import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemFullWithCardsBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemList2Binding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.ListingItemListWithCardsBinding
import com.idunnololz.summit.databinding.SearchResultPostItemBinding

enum class CommunityLayout {
  Compact,
  List,
  List2,
  LargeList,
  Card,
  Card2,
  Card3,
  Full,
  ListWithCards,
  FullWithCards,
  SmartList,
}

val CommunityLayout.usesDividers: Boolean
  get() =
    when (this) {
      CommunityLayout.Compact,
      CommunityLayout.List,
      CommunityLayout.List2,
      CommunityLayout.LargeList,
      -> true
      CommunityLayout.Card -> false
      CommunityLayout.Card2 -> false
      CommunityLayout.Card3 -> false
      CommunityLayout.Full -> true
      CommunityLayout.ListWithCards -> false
      CommunityLayout.FullWithCards -> false
      CommunityLayout.SmartList -> true
    }

val ViewBinding.communityLayout: CommunityLayout?
  get() =
    when (this) {
      is ListingItemCompactBinding -> CommunityLayout.Compact
      is ListingItemListBinding -> CommunityLayout.List
      is ListingItemList2Binding -> CommunityLayout.List2
      is ListingItemListWithCardsBinding -> CommunityLayout.ListWithCards
      is ListingItemFullWithCardsBinding -> CommunityLayout.FullWithCards
      is ListingItemCardBinding -> CommunityLayout.Card
      is ListingItemCard2Binding -> CommunityLayout.Card2
      is ListingItemCard3Binding -> CommunityLayout.Card3
      is ListingItemLargeListBinding -> CommunityLayout.LargeList
      is ListingItemFullBinding -> CommunityLayout.Full
      is SearchResultPostItemBinding -> null
      else -> null
    }
