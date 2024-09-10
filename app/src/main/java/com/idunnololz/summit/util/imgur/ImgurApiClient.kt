package com.idunnololz.summit.util.imgur

import android.net.Uri
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.FileTooLargeError
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class ImgurApiClient @Inject constructor(
    private val imgurApi: ImgurApi,
) {
    suspend fun uploadFile(file: File): Result<Upload> {
        return try{
            val filePart = MultipartBody.Part.createFormData(
                "image", file.name, file.asRequestBody())

            val response = imgurApi.uploadFile(
                authorization = "Client-ID ${ApiKeys.CLIENT_ID}",
                image = filePart,
                title =  file.name.toRequestBody()
            )

            if (response.isSuccessful) {
                Result.success(response.body()!!.upload)
            } else if (response.code() == 413) {
                Result.failure(FileTooLargeError())
            } else {
                Result.failure(ClientApiException(response.message(), response.code()))
            }
        }catch (e: Exception){
            Result.failure(e)
        }
    }
}