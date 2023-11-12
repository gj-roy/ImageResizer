package ru.tech.imageresizershrinker.presentation.filters_screen.components

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Texture
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.tech.imageresizershrinker.R
import ru.tech.imageresizershrinker.domain.image.ImageManager
import ru.tech.imageresizershrinker.domain.image.draw.DrawMode
import ru.tech.imageresizershrinker.domain.image.draw.pt
import ru.tech.imageresizershrinker.domain.image.filters.FilterMaskApplier
import ru.tech.imageresizershrinker.domain.model.ImageInfo
import ru.tech.imageresizershrinker.presentation.draw_screen.components.BitmapDrawer
import ru.tech.imageresizershrinker.presentation.draw_screen.components.BrushSoftnessSelector
import ru.tech.imageresizershrinker.presentation.draw_screen.components.DrawColorSelector
import ru.tech.imageresizershrinker.presentation.draw_screen.components.LineWidthSelector
import ru.tech.imageresizershrinker.presentation.root.icons.material.Eraser
import ru.tech.imageresizershrinker.presentation.root.model.PtSaver
import ru.tech.imageresizershrinker.presentation.root.model.UiPathPaint
import ru.tech.imageresizershrinker.presentation.root.model.toUiPathPaint
import ru.tech.imageresizershrinker.presentation.root.theme.mixedContainer
import ru.tech.imageresizershrinker.presentation.root.theme.onMixedContainer
import ru.tech.imageresizershrinker.presentation.root.theme.outlineVariant
import ru.tech.imageresizershrinker.presentation.root.transformation.filter.UiFilter
import ru.tech.imageresizershrinker.presentation.root.transformation.filter.toUiFilter
import ru.tech.imageresizershrinker.presentation.root.utils.state.update
import ru.tech.imageresizershrinker.presentation.root.widget.controls.EnhancedButton
import ru.tech.imageresizershrinker.presentation.root.widget.controls.EnhancedIconButton
import ru.tech.imageresizershrinker.presentation.root.widget.modifier.container
import ru.tech.imageresizershrinker.presentation.root.widget.other.Loading
import ru.tech.imageresizershrinker.presentation.root.widget.other.LocalToastHost
import ru.tech.imageresizershrinker.presentation.root.widget.other.showError
import ru.tech.imageresizershrinker.presentation.root.widget.sheets.SimpleSheet
import ru.tech.imageresizershrinker.presentation.root.widget.text.TitleItem
import ru.tech.imageresizershrinker.presentation.root.widget.utils.LocalWindowSizeClass
import ru.tech.imageresizershrinker.presentation.root.widget.utils.ScopedViewModelContainer
import javax.inject.Inject

@Composable
fun AddEditMaskSheet(
    mask: UiFilterMask? = null,
    visible: MutableState<Boolean>,
    targetBitmapUri: Uri? = null,
    masks: List<UiFilterMask> = emptyList(),
    onMaskPicked: (UiFilterMask) -> Unit
) {
    ScopedViewModelContainer<AddMaskSheetViewModel> { disposable ->
        val viewModel = this
        val imageManager = viewModel.getImageManager()
        val filterMaskApplier = viewModel.getFilterMaskApplier()

        LaunchedEffect(mask) {
            mask?.let {
                viewModel.setMask(mask)
            }
        }
        val portrait =
            LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE || LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact

        val showAddFilterSheet = rememberSaveable { mutableStateOf(false) }

        val context = LocalContext.current as ComponentActivity
        val toastHostState = LocalToastHost.current
        val scope = rememberCoroutineScope()

        val showReorderSheet = rememberSaveable { mutableStateOf(false) }

        var isEraserOn by rememberSaveable { mutableStateOf(false) }
        var strokeWidth by rememberSaveable(stateSaver = PtSaver) { mutableStateOf(20.pt) }
        var brushSoftness by rememberSaveable(stateSaver = PtSaver) { mutableStateOf(20.pt) }
        var zoomEnabled by rememberSaveable { mutableStateOf(false) }

        var transformedBitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(targetBitmapUri, mask, masks) {
            val bmp = imageManager.getImage(data = targetBitmapUri.toString(), false)
            if (targetBitmapUri != null && bmp != null) {
                transformedBitmap = filterMaskApplier.filterByMasks(
                    filterMasks = masks.takeWhile { it != mask },
                    image = bmp
                )?.let {
                    imageManager.createPreview(
                        image = it,
                        imageInfo = ImageInfo(width = it.width, height = it.height),
                        onGetByteCount = {}
                    )
                }
            }
        }

        SimpleSheet(
            visible = visible,
            title = {
                TitleItem(
                    text = stringResource(id = R.string.add_mask),
                    icon = Icons.Rounded.Texture
                )
            },
            confirmButton = {
                EnhancedButton(
                    enabled = viewModel.paths.isNotEmpty() && viewModel.filterList.isNotEmpty(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = {
                        onMaskPicked(
                            viewModel.getUiMask()
                        )
                        visible.value = false
                    }
                ) {
                    Text(stringResource(id = R.string.save))
                }
            }
        ) {
            disposable()
            val drawPreview: @Composable () -> Unit = {
                AnimatedContent(
                    targetState = remember(transformedBitmap) {
                        derivedStateOf {
                            transformedBitmap?.asImageBitmap()
                        }
                    }.value,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { imageBitmap ->
                    val maxHeight = LocalConfiguration.current.screenHeightDp.dp.minus(
                        WindowInsets.systemBars.asPaddingValues()
                            .run { calculateBottomPadding() + calculateTopPadding() }
                    ).minus(88.dp + 56.dp)
                    if (imageBitmap != null) {
                        val aspectRatio = imageBitmap.width / imageBitmap.height.toFloat()
                        BitmapDrawer(
                            imageBitmap = imageBitmap,
                            paths = viewModel.paths,
                            strokeWidth = strokeWidth,
                            brushSoftness = brushSoftness,
                            drawColor = viewModel.maskColor,
                            onAddPath = viewModel::addPath,
                            isEraserOn = isEraserOn,
                            drawMode = DrawMode.Pen,
                            modifier = Modifier
                                .padding(16.dp)
                                .then(
                                    if (portrait) {
                                        Modifier.height(maxHeight * (2 / 3f))
                                    } else Modifier.fillMaxHeight()
                                )
                                .aspectRatio(aspectRatio, portrait),
                            zoomEnabled = zoomEnabled,
                            onDraw = {},
                            imageManager = imageManager,
                            drawArrowsEnabled = false,
                            backgroundColor = Color.Transparent
                        )
                    } else {
                        Box(
                            modifier = Modifier.then(
                                if (portrait) {
                                    Modifier
                                        .height(maxHeight * (2 / 3f))
                                        .fillMaxWidth()
                                } else Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Loading()
                        }
                    }
                }
            }
            Row {
                if (!portrait) {
                    Box(modifier = Modifier.weight(1.3f)) {
                        drawPreview()
                    }
                    VerticalDivider()
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(
                            state = rememberScrollState()
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val switch = @Composable {
                        Switch(
                            modifier = Modifier.padding(start = 8.dp),
                            colors = SwitchDefaults.colors(
                                uncheckedBorderColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            checked = !zoomEnabled,
                            onCheckedChange = { zoomEnabled = !zoomEnabled },
                            thumbContent = {
                                AnimatedContent(zoomEnabled) { zoom ->
                                    Icon(
                                        if (!zoom) Icons.Rounded.Draw else Icons.Rounded.ZoomIn,
                                        null,
                                        Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            }
                        )
                    }

                    if (portrait) {
                        drawPreview()
                        HorizontalDivider()
                    }

                    Row(
                        Modifier
                            .padding(16.dp)
                            .container(shape = CircleShape)
                    ) {
                        switch()
                        EnhancedIconButton(
                            containerColor = Color.Transparent,
                            borderColor = MaterialTheme.colorScheme.outlineVariant(
                                luminance = 0.1f
                            ),
                            onClick = viewModel::undo,
                            enabled = viewModel.lastPaths.isNotEmpty() || viewModel.paths.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, null)
                        }
                        EnhancedIconButton(
                            containerColor = Color.Transparent,
                            borderColor = MaterialTheme.colorScheme.outlineVariant(
                                luminance = 0.1f
                            ),
                            onClick = viewModel::redo,
                            enabled = viewModel.undonePaths.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Redo, null)
                        }
                        EnhancedIconButton(
                            borderColor = MaterialTheme.colorScheme.outlineVariant(
                                luminance = 0.1f
                            ),
                            containerColor = animateColorAsState(
                                if (isEraserOn) MaterialTheme.colorScheme.mixedContainer
                                else Color.Transparent
                            ).value,
                            contentColor = animateColorAsState(
                                if (isEraserOn) MaterialTheme.colorScheme.onMixedContainer
                                else MaterialTheme.colorScheme.onSurface
                            ).value,
                            onClick = {
                                isEraserOn = !isEraserOn
                            }
                        ) {
                            Icon(Icons.Rounded.Eraser, null)
                        }
                    }

                    DrawColorSelector(
                        color = Color.Unspecified,
                        titleText = stringResource(id = R.string.mask_color),
                        defaultColors = remember {
                            listOf(
                                Color.Red,
                                Color.Green,
                                Color.Blue,
                                Color.Yellow,
                                Color.Cyan,
                                Color.Magenta
                            )
                        },
                        drawColor = viewModel.maskColor,
                        onColorChange = viewModel::updateMaskColor,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp
                        )
                    )
                    LineWidthSelector(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp
                        ),
                        color = Color.Unspecified,
                        value = strokeWidth.value,
                        onValueChange = { strokeWidth = it.pt }
                    )
                    BrushSoftnessSelector(
                        modifier = Modifier
                            .padding(top = 16.dp, end = 16.dp, start = 16.dp),
                        color = Color.Unspecified,
                        value = brushSoftness.value,
                        onValueChange = { brushSoftness = it.pt }
                    )
                    AnimatedContent(
                        targetState = viewModel.filterList.isNotEmpty(),
                        transitionSpec = {
                            fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
                        }
                    ) { notEmpty ->
                        if (notEmpty) {
                            Column(
                                Modifier
                                    .padding(16.dp)
                                    .container(MaterialTheme.shapes.extraLarge)
                            ) {
                                TitleItem(text = stringResource(R.string.filters))
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    viewModel.filterList.forEachIndexed { index, filter ->
                                        FilterItem(
                                            filter = filter,
                                            onFilterChange = {
                                                viewModel.updateFilter(
                                                    value = it,
                                                    index = index,
                                                    showError = {
                                                        scope.launch {
                                                            toastHostState.showError(
                                                                context = context,
                                                                error = it
                                                            )
                                                        }
                                                    }
                                                )
                                            },
                                            onLongPress = {
                                                showReorderSheet.value = true
                                            },
                                            showDragHandle = false,
                                            onRemove = {
                                                viewModel.removeFilterAtIndex(
                                                    index
                                                )
                                            }
                                        )
                                    }
                                    AddFilterButton(
                                        onClick = {
                                            showAddFilterSheet.value = true
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp
                                        )
                                    )
                                }
                            }
                        } else {
                            AddFilterButton(
                                onClick = {
                                    showAddFilterSheet.value = true
                                },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        AddFiltersSheet(
            visible = showAddFilterSheet,
            previewBitmap = null,
            onFilterPicked = { viewModel.addFilter(it.newInstance()) },
            onFilterPickedWithParams = { viewModel.addFilter(it) },
            imageManager = imageManager
        )
        FilterReorderSheet(
            filterList = viewModel.filterList,
            visible = showReorderSheet,
            updateOrder = viewModel::updateFiltersOrder
        )
    }
}

@HiltViewModel
class AddMaskSheetViewModel @Inject constructor(
    private val imageManager: ImageManager<Bitmap, ExifInterface>,
    private val filterMaskApplier: FilterMaskApplier<Bitmap, Path, Color>
) : ViewModel() {
    fun getImageManager(): ImageManager<Bitmap, ExifInterface> = imageManager

    fun getFilterMaskApplier(): FilterMaskApplier<Bitmap, Path, Color> = filterMaskApplier

    fun removeFilterAtIndex(index: Int) {
        _filterList.update {
            it.toMutableList().apply {
                removeAt(index)
            }
        }
    }

    fun <T : Any> updateFilter(
        value: T,
        index: Int,
        showError: (Throwable) -> Unit
    ) {
        val list = _filterList.value.toMutableList()
        runCatching {
            list[index] = list[index].copy(value)
            _filterList.update { list }
        }.exceptionOrNull()?.let { throwable ->
            showError(throwable)
            list[index] = list[index].newInstance()
            _filterList.update { list }
        }
    }

    fun updateFiltersOrder(uiFilters: List<UiFilter<*>>) {
        _filterList.update { uiFilters }
    }

    fun addFilter(filter: UiFilter<*>) {
        _filterList.update {
            it + filter
        }
    }

    fun getUiMask(): UiFilterMask = UiFilterMask(
        filters = filterList,
        maskPaints = paths
    )

    fun addPath(pathPaint: UiPathPaint) {
        _paths.update { it + pathPaint }
        _undonePaths.value = listOf()
    }

    fun undo() {
        if (paths.isEmpty() && lastPaths.isNotEmpty()) {
            _paths.value = lastPaths
            _lastPaths.value = listOf()
            return
        }
        if (paths.isEmpty()) return

        val lastPath = paths.last()

        _paths.update { it - lastPath }
        _undonePaths.update { it + lastPath }
    }

    fun redo() {
        if (undonePaths.isEmpty()) return

        val lastPath = undonePaths.last()
        _paths.update { it + lastPath }
        _undonePaths.update { it - lastPath }
    }

    fun updateMaskColor(color: Color) {
        _maskColor.update { color }
        _paths.update { paintList ->
            paintList.map {
                it.copy(drawColor = color)
            }
        }
    }

    fun setMask(mask: UiFilterMask) {
        _paths.update { mask.maskPaints.map { it.toUiPathPaint() } }
        _filterList.update { mask.filters.map { it.toUiFilter() } }
    }

    private val _maskColor = mutableStateOf(Color.Red)
    val maskColor by _maskColor

    private val _paths: MutableState<List<UiPathPaint>> = mutableStateOf(emptyList())
    val paths by _paths

    private val _lastPaths = mutableStateOf(listOf<UiPathPaint>())
    val lastPaths: List<UiPathPaint> by _lastPaths

    private val _undonePaths = mutableStateOf(listOf<UiPathPaint>())
    val undonePaths: List<UiPathPaint> by _undonePaths

    private val _filterList: MutableState<List<UiFilter<*>>> = mutableStateOf(emptyList())
    val filterList by _filterList

}