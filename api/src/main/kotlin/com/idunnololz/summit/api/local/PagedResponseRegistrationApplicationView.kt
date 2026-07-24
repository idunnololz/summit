package com.idunnololz.summit.api.local

class PagedResponseRegistrationApplicationView(

  val items: List<UserRegistrationApplication>,

  /* To get the next or previous page, pass this string unchanged as `page_cursor` in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. */
  val prevPage: String? = null,

  /* To get the next or previous page, pass this string unchanged as `page_cursor` in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. */
  val nextPage: String? = null,

)
