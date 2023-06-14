package com.ggstudios.lolcatalyst.scrape

/**
 * Exception that can be thrown or returned if a [WebsiteAdapter] fails. Useful when used with RX.
 */
class WebsiteAdapterException(
    errorId: Int
) : Exception("ErrorId: $errorId")