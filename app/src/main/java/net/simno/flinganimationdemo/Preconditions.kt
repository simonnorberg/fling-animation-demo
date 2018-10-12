package net.simno.flinganimationdemo

import android.os.Looper
import io.reactivex.Observer

fun Observer<*>.checkMainThread(): Boolean {
    return if (Looper.myLooper() != Looper.getMainLooper()) {
        onError(IllegalStateException("Expected to be called on the main thread but was " +
            Thread.currentThread().name))
        false
    } else {
        true
    }
}
