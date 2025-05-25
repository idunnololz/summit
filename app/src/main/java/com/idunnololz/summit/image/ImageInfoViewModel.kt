package com.idunnololz.summit.image

import android.content.Context
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.offline.TaskFailedListener
import com.idunnololz.summit.offline.TaskListener
import com.idunnololz.summit.util.FileSizeUtils
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.guessMimeType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ImageInfoViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val offlineManager: OfflineManager,
) : ViewModel() {

  val model = StatefulLiveData<ImageInfoModel>()

  fun loadImageInfo(url: String, force: Boolean = false) {
    model.setIsLoading()
    viewModelScope.launch {
      offlineManager.fetchImage(
        url = url,
        listener = object : TaskListener {
          override fun invoke(file: File) {
            loadExifData(file, url)
          }
        },
        errorListener = object : TaskFailedListener {
          override fun invoke(e: Throwable) {
            model.postError(e)
          }
        },
        force = force,
      )
    }
  }

  private fun loadExifData(file: File, url: String) {
    viewModelScope.launch {
      val items = mutableListOf<ImageInfoModel.Item>()
      val exifInterface = ExifInterface(file)

      items += ImageInfoModel.InfoItem(
        context.getString(R.string.url),
        url,
      )

      val fileSizeString = FileSizeUtils.convertToStringRepresentation(file.length())
      items += ImageInfoModel.InfoItem(
        title = context.getString(R.string.file_size),
        value = "$fileSizeString (${file.length()} bytes)",
      )

      items += ImageInfoModel.InfoItem(
        title = context.getString(R.string.file_type),
        value = try {
          guessMimeType(file)
            ?: context.getString(R.string.unknown)
        } catch (e: Exception) {
          context.getString(R.string.unknown)
        },
      )

      val options = BitmapFactory.Options()
      options.inJustDecodeBounds = true
      // Returns null, sizes are in the options variable
      BitmapFactory.decodeFile(file.path, options)

      items += ImageInfoModel.InfoItem(
        title = context.getString(R.string.image_size),
        value = "${options.outWidth} x ${options.outHeight}",
      )

//            val f = exifInterface.javaClass.getDeclaredField("mAttributes")
//            f.isAccessible = true
//            @Suppress("UNCHECKED_CAST")
//            val attributes: Array<HashMap<String, Any>> =
//                f.get(exifInterface) as Array<HashMap<String, Any>>
//
//            val tags = attributes.flatMap {
//                it.entries.map { (tag, _) ->
//                    tag
//                }
//            }
//
//            tags.mapTo(items) {
//                ImageInfoModel.InfoItem(
//                    it,
//                    exifInterface.getAttribute(it) ?: context.getString(R.string.unknown)
//                )
//            }

//            items.add(
//                ImageInfoModel.InfoItem(
//                    context.getString(R.string.file_created),
//                    ExifInterfaceUtils.parseDateTime(
//                        exifInterface.getAttribute(ExifInterface.TAG_DATETIME),
//                        exifInterface.getAttribute(ExifInterface.TAG_SUBSEC_TIME),
//                        exifInterface.getAttribute(ExifInterface.TAG_OFFSET_TIME),
//                    ).toString(),
//                ),
//            )

      model.postValue(
        ImageInfoModel(items),
      )
    }
  }
}
