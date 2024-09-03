package com.idunnololz.summit.util.imgur

import android.net.Uri
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
                filePart,
                name =  file.name.toRequestBody()
            )

            if(response.isSuccessful){
                Result.success(response.body()!!.upload)
            }else{
                Result.failure(Exception("Unknown network Exception."))
            }
        }catch (e: Exception){
            Result.failure(e)
        }
    }
}