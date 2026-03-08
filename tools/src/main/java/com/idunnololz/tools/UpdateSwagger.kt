package com.idunnololz.tools

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateSwagger {

  @JvmStatic
  fun main(args: Array<String>) {
    val outputFile = downloadSwaggerFile()
    val beforeFile = File("tools/swagger2.json")
    val swaggerFile = File("tools/swagger3.json")

    removeRequiredFromEnums(
      inputFile = outputFile,
      beforeFile = beforeFile,
      outputFile = swaggerFile,
    )

    // we can pause here to do a diff between swagger 2 and swagger 3

    swaggerFile.renameTo(File("tools/generatePieFedApi/swagger.json"))
    outputFile.delete()
    beforeFile.delete()
  }

  fun downloadSwaggerFile(): File {
    val okHttpClient = OkHttpClient()
    val request = Request.Builder().url("https://stable.wjs018.xyz/api/alpha/swagger.json")
      .get()
      .build()

    val call = okHttpClient.newCall(request).execute()
    val outputFile = File("tools/swagger.json")

    outputFile.outputStream().use {
      call.body?.byteStream()?.copyTo(it)
    }
    return outputFile
  }
}

fun removeRequiredFromEnums(inputFile: File, beforeFile: File, outputFile: File) {
  val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
  }

  val rootElement = json.parseToJsonElement(inputFile.readText())

  beforeFile.writeText(json.encodeToString(JsonElement.serializer(), rootElement))

  fun processElement(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> {
      val properties = element["properties"]
      val required = element["required"]

      var updatedObject = element

      if (properties is JsonObject && required is JsonArray) {
        val enumPropertyNames = properties
          .filterValues { it is JsonObject && it.containsKey("enum") }
          .keys

        if (enumPropertyNames.isNotEmpty()) {
          val newRequired = required.filterNot {
            it is JsonPrimitive && it.content in enumPropertyNames
          }

          updatedObject = if (newRequired.isEmpty()) {
            JsonObject(element - "required")
          } else {
            JsonObject(
              element.toMutableMap().apply {
                put("required", JsonArray(newRequired))
              },
            )
          }
        }
      }

      // Recursively process children
      JsonObject(
        updatedObject.mapValues { (_, value) ->
          processElement(value)
        },
      )
    }

    is JsonArray -> {
      JsonArray(element.map { processElement(it) })
    }

    else -> element
  }

  val updatedRoot = processElement(rootElement)

  outputFile.writeText(json.encodeToString(JsonElement.serializer(), updatedRoot))
}
