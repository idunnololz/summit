package com.idunnololz.summit.view.loadingIndicator;

import static androidx.core.math.MathUtils.clamp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.util.Property;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import com.google.android.material.animation.ArgbEvaluatorCompat;

public class LoadingIndicatorAnimatorDelegate {
  // Constants for animation timing.
  private static final int DURATION_PER_SHAPE_IN_MS = 650;

  // Constants for animation values.
  private static final int CONSTANT_ROTATION_PER_SHAPE_DEGREES = 50;
  private static final int EXTRA_ROTATION_PER_SHAPE_DEGREES = 90;
  private static final float SPRING_STIFFNESS = 200f;
  private static final float SPRING_DAMPING_RATIO = 0.6f;

  // For animator control.
  private int morphFactorTarget;
  private float animationFraction;
  private float morphFactor;

  private ObjectAnimator animator;
  private SpringAnimation springAnimation;

  @NonNull LoadingIndicatorSpec specs;
  @Nullable LoadingIndicatorDrawable drawable;
  LoadingIndicatorDrawingDelegate.IndicatorState indicatorState;

  public LoadingIndicatorAnimatorDelegate(@NonNull LoadingIndicatorSpec specs) {
    this.specs = specs;
    indicatorState = new LoadingIndicatorDrawingDelegate.IndicatorState();
  }

  /** Registers the drawable associated to this delegate. */
  protected void registerDrawable(@NonNull LoadingIndicatorDrawable drawable) {
    this.drawable = drawable;
  }

  void startAnimator() {
    maybeInitializeAnimators();

    resetPropertiesForNewStart();
    springAnimation.animateToFinalPosition(morphFactorTarget);
    animator.start();
  }

  void cancelAnimatorImmediately() {
    if (animator != null) {
      animator.cancel();
    }
    if (springAnimation != null) {
      springAnimation.skipToEnd();
    }
  }

  void invalidateSpecValues() {
    resetPropertiesForNewStart();
  }

  void resetPropertiesForNewStart() {
    morphFactorTarget = 1;
    setMorphFactor(0);
    indicatorState.color = specs.indicatorColors[0];
  }

  private void maybeInitializeAnimators() {
    if (springAnimation == null) {
      springAnimation =
          new SpringAnimation(this, MORPH_FACTOR)
              .setSpring(
                  new SpringForce()
                      .setStiffness(SPRING_STIFFNESS)
                      .setDampingRatio(SPRING_DAMPING_RATIO))
              .setMinimumVisibleChange(0.01f);
    }
    if (animator == null) {
      // Instantiates an animator with the linear interpolator to control the animation progress.
      animator = ObjectAnimator.ofFloat(this, ANIMATION_FRACTION, 0, 1);
      animator.setDuration(DURATION_PER_SHAPE_IN_MS);
      animator.setInterpolator(null);
      animator.setRepeatCount(ValueAnimator.INFINITE);
      animator.addListener(
          new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
              super.onAnimationRepeat(animation);
              springAnimation.animateToFinalPosition(++morphFactorTarget);
            }
          });
    }
  }

  // ******************* Helper methods *******************

  /** Updates the indicator's rotation based on current playtime. */
  private void updateIndicatorRotation(int playtime) {
    float morphFactorBase = morphFactorTarget - 1;
    float morphFactorPerShape = morphFactor - morphFactorBase;
    float timeFactorPerShape = (float) playtime / DURATION_PER_SHAPE_IN_MS;
    if (timeFactorPerShape == 1f) {
      // The animation on repeat is called before the playtime restart. So if playtime reaches the
      // end, we take it as restarted as 0.
      timeFactorPerShape = 0f;
    }
    // Initial rotation.
    indicatorState.rotationDegree =
        (CONSTANT_ROTATION_PER_SHAPE_DEGREES + EXTRA_ROTATION_PER_SHAPE_DEGREES) * morphFactorBase;
    // Constant rotation.
    indicatorState.rotationDegree += CONSTANT_ROTATION_PER_SHAPE_DEGREES * timeFactorPerShape;
    // Rotation driven by spring animation.
    indicatorState.rotationDegree += EXTRA_ROTATION_PER_SHAPE_DEGREES * morphFactorPerShape;

    indicatorState.rotationDegree %= 360;
  }

  /** Updates the indicator's shape and color driven by spring animation. */
  private void updateIndicatorShapeAndColor() {
    indicatorState.morphFraction = morphFactor;
    // Updates color.
    int startColorIndex = (morphFactorTarget - 1) % specs.indicatorColors.length;
    int endColorIndex = (startColorIndex + 1) % specs.indicatorColors.length;
    int startColor = specs.indicatorColors[startColorIndex];
    int endColor = specs.indicatorColors[endColorIndex];
    indicatorState.color =
        ArgbEvaluatorCompat.getInstance()
            .evaluate(clamp(morphFactor - (morphFactorTarget - 1), 0, 1), startColor, endColor);
  }

  // ******************* Getters and setters *******************

  private float getAnimationFraction() {
    return animationFraction;
  }

  @VisibleForTesting
  void setAnimationFraction(float fraction) {
    animationFraction = fraction;
    int playtime = (int) (animationFraction * DURATION_PER_SHAPE_IN_MS);
    updateIndicatorRotation(playtime);
    if (drawable != null) {
      drawable.invalidateSelf();
    }
  }

  private float getMorphFactor() {
    return morphFactor;
  }

  void setMorphFactor(float factor) {
    morphFactor = factor;
    updateIndicatorShapeAndColor();
    if (drawable != null) {
      drawable.invalidateSelf();
    }
  }

  void setMorphFactorTarget(int factorTarget) {
    morphFactorTarget = factorTarget;
  }

  // ******************* Properties *******************

  private static final Property<LoadingIndicatorAnimatorDelegate, Float> ANIMATION_FRACTION =
      new Property<LoadingIndicatorAnimatorDelegate, Float>(Float.class, "animationFraction") {
        @Override
        public Float get(LoadingIndicatorAnimatorDelegate delegate) {
          return delegate.getAnimationFraction();
        }

        @Override
        public void set(LoadingIndicatorAnimatorDelegate delegate, Float value) {
          delegate.setAnimationFraction(value);
        }
      };

  private static final FloatPropertyCompat<LoadingIndicatorAnimatorDelegate> MORPH_FACTOR =
      new FloatPropertyCompat<LoadingIndicatorAnimatorDelegate>("morphFactor") {
        @Override
        public float getValue(LoadingIndicatorAnimatorDelegate delegate) {
          return delegate.getMorphFactor();
        }

        @Override
        public void setValue(LoadingIndicatorAnimatorDelegate delegate, float value) {
          delegate.setMorphFactor(value);
        }
      };
}
