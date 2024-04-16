package com.idunnololz.summit.util.recyclerView

import java.util.concurrent.atomic.AtomicInteger

private const val INITIAL_BASE_VALUE = 0x10000000
private const val VIEW_TYPE_GENERATOR_MAX_VALUES = 0x1000

object ViewTypeManager {
    private var viewTypeGeneratorCount = AtomicInteger(0)

    fun create() = ViewTypeGenerator(
        INITIAL_BASE_VALUE +
            (viewTypeGeneratorCount.getAndIncrement() * VIEW_TYPE_GENERATOR_MAX_VALUES),
    )
}

class ViewTypeGenerator(
    private val baseType: Int,
) {
    private var nextViewType = AtomicInteger(0)

    fun generateType(): Int = baseType + nextViewType.getAndIncrement()
}
