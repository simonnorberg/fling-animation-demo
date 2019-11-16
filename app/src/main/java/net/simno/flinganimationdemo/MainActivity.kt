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
import net.simno.flinganimationdemo.CircleView.Companion.MAX_FRICTION
import net.simno.flinganimationdemo.databinding.MainActivityBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val binding: MainActivityBinding by lazy {
        MainActivityBinding.inflate(layoutInflater)
    }
    private val onError: (Throwable) -> Unit = { Log.e(TAG, it.message, it) }
    private var disposables: CompositeDisposable? = null
    private var position = PointF(0.5f, 0.5f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
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
            binding.circleView.setPosition(position)
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

        disposables += binding.frictionSeekBar.changes()
            .map { if (it >= 30) MAX_FRICTION else (it + 1) / 10f }
            .doOnNext { binding.circleView.friction = it }
            .map { if (it == MAX_FRICTION) "∞" else it.toString() }
            .subscribe({ binding.frictionValue.text = it }, onError)

        disposables += binding.circleView.positions()
            .subscribe({
                position = it
                binding.xValue.text = it.x.format()
                binding.yValue.text = it.y.format()
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
