package net.simno.flinganimationdemo

import android.content.Intent
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.net.toUri
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.jakewharton.rxbinding2.view.globalLayouts
import com.jakewharton.rxbinding2.widget.changes
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_main.circleView
import kotlinx.android.synthetic.main.activity_main.frictionSeekBar
import kotlinx.android.synthetic.main.activity_main.frictionText
import kotlinx.android.synthetic.main.activity_main.xText
import kotlinx.android.synthetic.main.activity_main.yText
import net.simno.flinganimationdemo.CircleView.Companion.MAX_FRICTION
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val main: Scheduler = AndroidSchedulers.mainThread()
    private val onError: (Throwable) -> Unit = { Log.e(TAG, it.message, it) }
    private var disposables: CompositeDisposable? = null
    private var position = PointF(0.5f, 0.5f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        frictionSeekBar.progress = 9
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState?.let {
            if (it.containsKey(POSITION_KEY)) {
                position = it.getParcelable(POSITION_KEY)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(POSITION_KEY, position)
    }

    override fun onStart() {
        super.onStart()
        disposables = observeUi()
    }

    override fun onStop() {
        super.onStop()
        disposables?.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_source -> {
                val url = "https://github.com/simonnorberg/fling-animation-demo".toUri()
                CustomTabsIntent.Builder()
                        .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        .build()
                        .apply {
                            if (isAvailable(url)) {
                                launchUrl(this@MainActivity, url)
                            }
                        }
                true
            }
            R.id.menu_licenses -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeUi(): CompositeDisposable {
        val disposables = CompositeDisposable()

        disposables += frictionSeekBar.changes()
                .applySchedulers()
                .subscribe({ progress ->
                    val friction = progress.toFriction()
                    frictionText.text = friction.frictionText()
                    circleView.friction = friction
                }, onError)

        disposables += circleView.positions()
                .applySchedulers()
                .subscribe({ position ->
                    this@MainActivity.position = position
                    xText.text = getPositionText(R.string.x, position.x)
                    yText.text = getPositionText(R.string.y, position.y)
                }, onError)

        disposables += circleView.globalLayouts()
                .applySchedulers()
                .subscribe({
                    circleView.setPosition(position)
                }, onError)

        return disposables
    }

    private fun <T> Observable<T>.applySchedulers(): Observable<T> =
            subscribeOn(main).observeOn(main)

    private fun CustomTabsIntent.isAvailable(url: Uri): Boolean {
        this.intent.data = url
        return packageManager.queryIntentActivities(this.intent, MATCH_DEFAULT_ONLY).size > 0
    }

    private fun CircleView.positions(): Observable<PointF> = CircleViewObservable(this)

    private fun Int.toFriction(): Float = if (this >= 30) MAX_FRICTION else (this + 1) / 10f

    private fun Float.frictionText(): String =
            getString(R.string.friction, if (this == MAX_FRICTION) "MAX" else this.toString())

    private fun getPositionText(resId: Int, value: Float) =
            getString(resId, String.format(Locale.US, "%.02f", value))

    companion object {
        private const val POSITION_KEY = "position"
        private const val TAG = "MainActivity"
    }
}
