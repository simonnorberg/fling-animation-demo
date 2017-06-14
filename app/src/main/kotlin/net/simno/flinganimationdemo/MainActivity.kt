package net.simno.flinganimationdemo

import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.jakewharton.rxbinding2.view.globalLayouts
import com.jakewharton.rxbinding2.widget.changes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.circleView
import kotlinx.android.synthetic.main.activity_main.frictionSeekBar
import kotlinx.android.synthetic.main.activity_main.frictionText
import kotlinx.android.synthetic.main.activity_main.xText
import kotlinx.android.synthetic.main.activity_main.yText
import java.util.Locale

class MainActivity : AppCompatActivity() {

  private val TAG = "MainActivity"
  private val POSITION = "position"

  private var disposables: CompositeDisposable? = null
  var position = PointF(0.5f, 0.5f)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    frictionSeekBar.progress = 9
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
    super.onRestoreInstanceState(savedInstanceState)
    if (savedInstanceState != null && savedInstanceState.containsKey(POSITION)) {
      position = savedInstanceState.getParcelable<PointF>(POSITION)
    }
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    super.onSaveInstanceState(outState)
    outState?.putParcelable(POSITION, position)
  }

  override fun onStart() {
    super.onStart()
    observe()
  }

  override fun onStop() {
    super.onStop()
    dispose()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    if (item?.itemId == R.id.menu_source) {
      CustomTabsIntent.Builder()
          .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
          .build()
          .launchUrl(this, Uri.parse("https://github.com/simonnorberg/fling-animation-demo"))
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun observe() {
    disposables = CompositeDisposable()
    disposables?.add(frictionSeekBar.changes()
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ progress ->
          val friction = if (progress >= 30) CircleView.MAX_FRICTION else (progress + 1) / 10f

          frictionText.text = getString(R.string.friction,
              if (friction == CircleView.MAX_FRICTION) "MAX" else friction.toString())

          circleView.friction = friction
        }, { error ->
          Log.e(TAG, error.message, error)
        }))

    disposables?.add(circleView.positions()
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ position ->
          this@MainActivity.position = position
          xText.text = getString(R.string.x, String.format(Locale.US, "%.02f", position.x))
          yText.text = getString(R.string.y, String.format(Locale.US, "%.02f", position.y))
        }, { error ->
          Log.e(TAG, error.message, error)
        }))

    disposables?.add(circleView.globalLayouts()
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          circleView.setPosition(position)
        }, { error ->
          Log.e(TAG, error.message, error)
        }))
  }

  private fun dispose() {
    disposables?.dispose()
  }

  fun CircleView.positions(): Observable<PointF> = CircleViewObservable(this)
}
