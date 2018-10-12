package net.simno.flinganimationdemo

import android.graphics.PointF
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

class CircleViewObservable(private val view: CircleView) : Observable<PointF>() {
    override fun subscribeActual(observer: Observer<in PointF>) {
        if (!observer.checkMainThread()) {
            return
        }
        Listener(view, observer).let {
            observer.onSubscribe(it)
            view.onPositionChangedListener = it
        }
    }

    internal class Listener(
        private val view: CircleView,
        private val observer: Observer<in PointF>
    ) : MainThreadDisposable(), CircleView.OnPositionChangedListener {

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
