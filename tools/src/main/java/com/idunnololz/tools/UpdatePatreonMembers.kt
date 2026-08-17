package com.idunnololz.tools

import java.io.File
import java.util.Properties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

object UpdatePatreonMembers {

  private const val PAGE_SIZE = 1000

  data class PatreonMember(
    val name: String,
    val pledge: Long,
  )

  @JvmStatic
  fun main(args: Array<String>) {
    val properties = Properties()
    File("patreon.properties").inputStream().use {
      properties.load(it)
    }

    val campaignId = properties.getProperty("campaign.id")
      ?: error("campaign.id is missing from patreon.properties")
    val authToken = properties.getProperty("auth.token")
      ?: error("auth.token is missing from patreon.properties")

    val api = Retrofit.Builder()
      .baseUrl("https://www.patreon.com/")
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(PatreonApi::class.java)

    val members = fetchAllMembers(api, campaignId, authToken)
      .mapNotNull { resource ->
        val attributes = resource.attributes ?: return@mapNotNull null
        val name = attributes.fullName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

        PatreonMember(
          name = name,
          pledge = attributes.currentlyEntitledAmountCents ?: 0,
        )
      }

    val paidMembers = members
      .filter { it.pledge > 0 }
      .sortedByDescending { it.pledge }

    val patreonFile = File("app/src/main/res/raw/patreon.txt")
    patreonFile.outputStream().bufferedWriter().use {
      it.appendLine(paidMembers.joinToString(separator = "\n") { member -> member.name })
    }

    println("Updated ${patreonFile.name} with ${paidMembers.size} members.")
    println(patreonFile.absolutePath)
  }

  private fun fetchAllMembers(
    api: PatreonApi,
    campaignId: String,
    authToken: String,
  ): List<PatreonMemberResource> {
    val members = mutableListOf<PatreonMemberResource>()
    val seenCursors = mutableSetOf<String>()
    var cursor: String? = null

    do {
      val response = api.getCampaignMembers(
        campaignId = campaignId,
        authorization = "Bearer $authToken",
        fields = "full_name,currently_entitled_amount_cents",
        pageSize = PAGE_SIZE,
        cursor = cursor,
      ).execute()

      if (!response.isSuccessful) {
        val errorBody = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
        error(
          buildString {
            append("Patreon API request failed with HTTP ${response.code()}")
            if (errorBody != null) append(": $errorBody")
          },
        )
      }

      val page = response.body() ?: error("Patreon API returned an empty response body")
      members.addAll(page.data)
      cursor = page.meta?.pagination?.cursors?.next

      if (cursor != null && !seenCursors.add(cursor)) {
        error("Patreon API returned the same pagination cursor more than once")
      }
    } while (cursor != null)

    return members
  }

  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }
}

private interface PatreonApi {
  @GET("api/oauth2/v2/campaigns/{campaignId}/members")
  fun getCampaignMembers(
    @Path("campaignId") campaignId: String,
    @Header("Authorization") authorization: String,
    @Query("fields[member]") fields: String,
    @Query("page[count]") pageSize: Int,
    @Query("page[cursor]") cursor: String?,
  ): Call<PatreonMembersResponse>
}

@Serializable
private data class PatreonMembersResponse(
  val data: List<PatreonMemberResource> = emptyList(),
  val meta: PatreonMeta? = null,
)

@Serializable
private data class PatreonMemberResource(
  val attributes: PatreonMemberAttributes? = null,
)

@Serializable
private data class PatreonMemberAttributes(
  @SerialName("full_name")
  val fullName: String? = null,
  @SerialName("currently_entitled_amount_cents")
  val currentlyEntitledAmountCents: Long? = null,
)

@Serializable
private data class PatreonMeta(
  val pagination: PatreonPagination? = null,
)

@Serializable
private data class PatreonPagination(
  val cursors: PatreonCursors? = null,
)

@Serializable
private data class PatreonCursors(
  val next: String? = null,
)
