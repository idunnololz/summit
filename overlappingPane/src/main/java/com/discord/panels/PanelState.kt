package com.discord.panels

sealed class PanelState {
  data class Opening(val offset: Float, val progress: Float) : PanelState()
  object Opened : PanelState()
  data class Closing(val offset: Float, val progress: Float) : PanelState()
  object Closed : PanelState()
}
