package net.simno.flinganimationdemo

import android.graphics.PointF
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import net.simno.flinganimationdemo.Preconditions.checkMainThread

class CircleViewObservable(private val view: CircleView) : Observable<PointF>() {

  override fun subscribeActual(observer: Observer<in PointF>) {
    if (!observer.checkMainThread()) {
      return
    }
    val listener = Listener(view, observer)
    observer.onSubscribe(listener)
    view.onPositionChangedListener = listener
  }

  internal class Listener(val view: CircleView, val observer: Observer<in PointF>)
    : MainThreadDisposable(), CircleView.OnPositionChangedListener {

    override fun onPositionChanged(point: PointF) {
      if (!isDisposed) {
        try {
          observer.onNext(point)
        } catch (e: Exception) {
          observer.onError(e)
          dispose()
        }
      }
    }

    override fun onDispose() {
      view.onPositionChangedListener = null
    }
  }
}
