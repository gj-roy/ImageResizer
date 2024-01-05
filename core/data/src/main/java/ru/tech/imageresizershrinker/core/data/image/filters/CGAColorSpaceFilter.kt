package ru.tech.imageresizershrinker.core.data.image.filters

import android.content.Context
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.filter.GPUImageCGAColorspaceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import ru.tech.imageresizershrinker.core.domain.image.filters.Filter


class CGAColorSpaceFilter(
    private val context: Context,
    override val value: Unit = Unit
) : GPUFilterTransformation(context), Filter.CGAColorSpace<Bitmap> {
    override val cacheKey: String
        get() = (value to context).hashCode().toString()

    override fun createFilter(): GPUImageFilter = GPUImageCGAColorspaceFilter()
}