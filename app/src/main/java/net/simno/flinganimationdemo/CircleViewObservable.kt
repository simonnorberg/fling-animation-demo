package net.simno.flinganimationdemo

import android.graphics.PointF
import androidx.annotation.CheckResult
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

@CheckResult
fun CircleView.positions(): Observable<PointF> {
    return CircleViewObservable(this)
}

private class CircleViewObservable(
    private val view: CircleView
) : Observable<PointF>() {

    override fun subscribeActual(observer: Observer<in PointF>) {
        if (!checkMainThread(observer)) {
            return
        }
        val listener = Listener(view, observer)
        observer.onSubscribe(listener)
        view.onPositionChangedListener = listener
    }

    private class Listener(
        private val view: CircleView,
        private val observer: Observer<in PointF>
    ) : MainThreadDisposable(), CircleView.OnPositionChangedListener {

        override fun onPositionChanged(point: PointF) {
            if (!isDisposed) {
                observer.onNext(point)
            }
        }

        override fun onDispose() {
            view.onPositionChangedListener = null
        }
    }
}
