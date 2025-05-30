package com.discord.panels

sealed class PanelState {
  data class Opening(val offset: Float, val progress: Float) : PanelState()
  data object Opened : PanelState()
  data class Closing(val offset: Float, val progress: Float) : PanelState()
  data object Closed : PanelState()
}
