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

package ru.tech.imageresizershrinker.core.data.saving

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.coroutineScope
import ru.tech.imageresizershrinker.core.domain.saving.model.SaveTarget
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

internal data class SavingFolder(
    val outputStream: OutputStream? = null,
    val fileUri: Uri? = null
) {
    companion object {
        suspend fun getInstance(
            context: Context,
            treeUri: Uri?,
            saveTarget: SaveTarget
        ): SavingFolder = coroutineScope {
            if (treeUri == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val type = saveTarget.mimeType
                    val path = "${Environment.DIRECTORY_DOCUMENTS}/ResizedImages"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, saveTarget.filename)
                        put(
                            MediaStore.MediaColumns.MIME_TYPE,
                            type
                        )
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            path
                        )
                    }
                    val imageUri = context.contentResolver.insert(
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        contentValues
                    )

                    SavingFolder(
                        outputStream = imageUri?.let {
                            context.contentResolver.openOutputStream(
                                it
                            )
                        },
                        fileUri = imageUri
                    )
                } else {
                    val imagesDir = File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS
                        ), "ResizedImages"
                    )
                    if (!imagesDir.exists()) imagesDir.mkdir()
                    SavingFolder(
                        outputStream = saveTarget.filename?.let {
                            FileOutputStream(File(imagesDir, it))
                        },
                        fileUri = saveTarget.filename?.let { File(imagesDir, it).toUri() }
                    )
                }
            } else if (DocumentFile.isDocumentUri(context, treeUri)) {
                SavingFolder(
                    outputStream = context.contentResolver.openOutputStream(treeUri),
                    fileUri = treeUri
                )
            } else {
                val documentFile = DocumentFile.fromTreeUri(context, treeUri)

                if (documentFile?.exists() == false || documentFile == null) {
                    throw NoSuchFileException(File(treeUri.toString()))
                }

                val file =
                    documentFile.createFile(saveTarget.mimeType, saveTarget.filename!!)

                val imageUri = file!!.uri
                SavingFolder(
                    outputStream = context.contentResolver.openOutputStream(imageUri),
                    fileUri = imageUri
                )
            }
        }
    }
}