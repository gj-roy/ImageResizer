/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package ru.tech.imageresizershrinker.core.ui.shapes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ru.tech.imageresizershrinker.core.settings.presentation.IconShape
import ru.tech.imageresizershrinker.core.settings.presentation.LocalSettingsState
import ru.tech.imageresizershrinker.core.ui.theme.inverse
import ru.tech.imageresizershrinker.core.ui.widget.modifier.container

val IconShapesList by lazy {
    persistentListOf(
        IconShape(SquircleShape),
        IconShape(CloverShape, 4.dp),
        IconShape(MaterialStarShape, 6.dp, 22.dp),
        IconShape(OvalShape, 6.dp),
        IconShape(PentagonShape, 6.dp, 22.dp),
        IconShape(OctagonShape, 6.dp, 22.dp),
        IconShape(HeartShape, 10.dp, 18.dp),
    )
}

@Composable
fun IconShapeContainer(
    enabled: Boolean,
    underlyingColor: Color,
    iconShape: IconShape? = LocalSettingsState.current.iconShape,
    content: @Composable (Boolean) -> Unit = {}
) {
    Box(
        modifier = if (enabled && iconShape != null) {
            Modifier.container(
                shape = iconShape.shape,
                color = underlyingColor.inverse(
                    fraction = { if (it) 0.15f else 0.1f }
                ),
                resultPadding = iconShape.padding
            )
        } else Modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = if (enabled && iconShape != null) {
                Modifier
                    .size(iconShape.iconSize)
                    .offset(
                        y = if (iconShape.shape == PentagonShape) 2.dp
                        else 0.dp
                    )
            } else Modifier
        ) {
            content(iconShape == null)
        }
    }
}