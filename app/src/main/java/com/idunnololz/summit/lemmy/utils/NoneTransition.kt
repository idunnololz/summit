package com.idunnololz.summit.lemmy.utils

import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.transition.Transition
import coil3.transition.TransitionTarget

val NoneTransitionFactory = NoneTransition.Factory()

class NoneTransition(
  private val target: TransitionTarget,
  private val result: ImageResult,
) : Transition {

  override fun transition() {
    when (result) {
      is SuccessResult -> target.onSuccess(result.image)
      is ErrorResult -> target.onError(result.image)
    }
  }

  class Factory : Transition.Factory {

    override fun create(target: TransitionTarget, result: ImageResult): Transition =
      NoneTransition(target, result)
  }
}
