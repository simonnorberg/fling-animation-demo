package net.simno.flinganimationdemo

import android.content.Intent
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.jakewharton.rxbinding3.widget.changes
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_main.circleView
import kotlinx.android.synthetic.main.activity_main.frictionSeekBar
import kotlinx.android.synthetic.main.activity_main.frictionValue
import kotlinx.android.synthetic.main.activity_main.xValue
import kotlinx.android.synthetic.main.activity_main.yValue
import net.simno.flinganimationdemo.CircleView.Companion.MAX_FRICTION
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val onError: (Throwable) -> Unit = { Log.e(TAG, it.message, it) }
    private var disposables: CompositeDisposable? = null
    private var position = PointF(0.5f, 0.5f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        disposables = observeViews()
    }

    override fun onStop() {
        super.onStop()
        disposables?.clear()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.containsKey(POSITION_KEY)) {
            position = savedInstanceState.getParcelable(POSITION_KEY) ?: position
            circleView.setPosition(position)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(POSITION_KEY, position)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_source -> {
                val customTabs = CustomTabsIntent.Builder().build()
                customTabs.intent.data = url
                if (packageManager.resolveActivity(customTabs.intent, MATCH_DEFAULT_ONLY) != null) {
                    customTabs.launchUrl(this, url)
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

    private fun observeViews(): CompositeDisposable {
        val disposables = CompositeDisposable()

        disposables += frictionSeekBar.changes()
            .map { if (it >= 30) MAX_FRICTION else (it + 1) / 10f }
            .doOnNext { circleView.friction = it }
            .map { if (it == MAX_FRICTION) "∞" else it.toString() }
            .subscribe({ frictionValue.text = it }, onError)

        disposables += circleView.positions()
            .subscribe({
                position = it
                xValue.text = it.x.format()
                yValue.text = it.y.format()
            }, onError)

        return disposables
    }

    private fun Float.format() = String.format(Locale.US, "%.02f", this)

    companion object {
        private const val POSITION_KEY = "position"
        private const val TAG = "MainActivity"
        private val url = "https://github.com/simonnorberg/fling-animation-demo".toUri()
    }
}
