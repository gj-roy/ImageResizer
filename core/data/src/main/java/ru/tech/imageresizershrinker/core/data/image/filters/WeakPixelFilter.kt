package ru.tech.imageresizershrinker.core.data.image.filters

import android.content.Context
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWeakPixelInclusionFilter
import ru.tech.imageresizershrinker.core.domain.image.filters.Filter


class WeakPixelFilter(
    private val context: Context,
    override val value: Unit = Unit
) : GPUFilterTransformation(context), Filter.WeakPixel<Bitmap> {
    override val cacheKey: String
        get() = (value to context).hashCode().toString()

    override fun createFilter(): GPUImageFilter = GPUImageWeakPixelInclusionFilter()
}