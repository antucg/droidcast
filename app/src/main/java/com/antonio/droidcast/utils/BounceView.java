package com.antonio.droidcast.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.BounceInterpolator;

/**
 * Created by antonio.carrasco on 13/05/2017.
 */

public class BounceView {
  public static void animate(View v, final Runnable callback) {
    ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.5f, 1);
    ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.5f, 1);
    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.setDuration(500);
    animatorSet.setInterpolator(new BounceInterpolator());
    animatorSet.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animation) {

      }

      @Override public void onAnimationEnd(Animator animation) {
        if (callback != null) {
          callback.run();
        }
      }

      @Override public void onAnimationCancel(Animator animation) {

      }

      @Override public void onAnimationRepeat(Animator animation) {

      }
    });
    animatorSet.play(scaleX).with(scaleY);
    animatorSet.start();
  }
}