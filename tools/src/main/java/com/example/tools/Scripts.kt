package com.example.tools

import com.patreon.PatreonAPI
import java.io.File
import java.util.Properties

object UpdatePatreonMembers {

  data class PatreonMember(
    val name: String,
    val pledge: Int,
  )

  @JvmStatic
  fun main(args: Array<String>) {
    val properties = Properties()
    File("patreon.properties").inputStream().use {
      properties.load(it)
    }

    val campaignId = properties.getProperty("campaign.id")
    val authToken = properties.getProperty("auth.token")

    val apiClient = PatreonAPI(authToken)
    val members = mutableListOf<PatreonMember>()
    apiClient.fetchAllPledges(campaignId).forEach {
      members.add(
        PatreonMember(
          it.patron.fullName,
          it.pledgeCapCents,
        ),
      )
    }

    val patreonFile = File("app/src/main/res/raw/patreon.txt")
    patreonFile.outputStream().bufferedWriter().use {
      it.appendLine(
        members
          .filter { it.pledge > 0 }
          .sortedByDescending { it.pledge }
          .joinToString(separator = "\n") { it.name },
      )
    }

    println("Updated ${patreonFile.name} with ${members.size} members.")
    println(patreonFile.absolutePath)
  }
}
