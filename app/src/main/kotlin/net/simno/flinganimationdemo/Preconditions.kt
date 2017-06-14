package net.simno.flinganimationdemo

import android.os.Looper
import io.reactivex.Observer

object Preconditions {
  fun Observer<*>.checkMainThread(): Boolean {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      onError(IllegalStateException(
          "Expected to be called on the main thread but was " + Thread.currentThread().name))
      return false
    }
    return true
  }
}
