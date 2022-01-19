package com.safta.myapplication

import androidx.appcompat.app.AppCompatActivity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import org.json.JSONTokener

class MainActivity : AppCompatActivity() {

    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder: Notification.Builder
    private val channelId = "i.apps.notifications"
    private val description = "Test notification"
    lateinit var textView :TextView
    lateinit var textView2 :TextView
    lateinit var getButton :Button
var i  = 10
    private lateinit var mqttClient: MqttAndroidClient
    // TAG
    companion object {
        const val TAG = "AndroidMqttClient"
    }

    var myindex = -1
    var date = ""
    var index = -3

    val url = "https://iotsmartaq.herokuapp.com/init"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        textView = findViewById<TextView>(R.id.textView)
        textView2 = findViewById<TextView>(R.id.textView2)
        getButton = findViewById<Button>(R.id.button)
        getData()
        httpGet()

        //connect(this)
        // subscribe("safta/med")
        //  publish("safta/med","hello safta")




    }
fun httpGet(){
    val queue = Volley.newRequestQueue(this)
    val stringRequest = StringRequest(Request.Method.GET, url,
        { response ->
            // Display the first 500 characters of the response string.
            val jsonObject = JSONTokener(response.toString()).nextValue() as JSONObject
            index = jsonObject.getString("index").toInt()
            val dateReq = jsonObject.getString("Day") + jsonObject.getString("round")
           // Toast.makeText(applicationContext, "${(myindex).toString()}", Toast.LENGTH_LONG).show()

            val lindex = jsonObject.getString("finalindex").toInt()
            if ((dateReq == date)&&(index<myindex)){
                connect(this)
                getButton.visibility = View.INVISIBLE
                textView2.text = myindex.toString()
            }else{
                getButton.visibility = View.VISIBLE
                textView2.text = lindex.toString()
            }
            textView.text = index.toString()



        },
        { textView.text = "404" })
    queue.add(stringRequest)
}
    fun getnum(v:View){
        val url2 = "https://iotsmartaq.herokuapp.com/add"
        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(Request.Method.GET, url2,
            { response ->
                // Display the first 500 characters of the response string.
                val jsonObject = JSONTokener(response.toString()).nextValue() as JSONObject
                index = jsonObject.getString("index").toInt()
                 date = jsonObject.getString("Day") + jsonObject.getString("round")
                val lindex = jsonObject.getString("finalindex").toInt()

                saveData(lindex,date)
                getData()
                textView.text = index.toString()
                textView2.text = lindex.toString()
                getButton.visibility = View.INVISIBLE
                connect(this)

            },
            { textView.text = "404" })
        queue.add(stringRequest)
    }
    fun saveData(indexVal :Int,dateVal : String) {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("date", dateVal)
            putInt("index", indexVal)
            commit()
        }
    }

    fun getData() {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        date = sharedPref.getString("date", "").toString()
        myindex = sharedPref.getInt("index", 0).toInt()
        //Toast.makeText(this, "$str_name $int_number", Toast.LENGTH_LONG).show()
    }


    fun connect(context: Context) {
        val serverURI = "tcp://aef3f4de.us-east-1.emqx.cloud:15094"
        mqttClient = MqttAndroidClient(context, serverURI, "safta_nova")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Toast.makeText(applicationContext, "Receive message: ${message.toString()} from topic: $topic", Toast.LENGTH_LONG).show()
                val jsonObject = JSONTokener(message.toString()).nextValue() as JSONObject
                val index = jsonObject.getString("index").toInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
                    notificationChannel.enableLights(true)
                    notificationChannel.lightColor = Color.GREEN
                    notificationChannel.enableVibration(false)
                    notificationManager.createNotificationChannel(notificationChannel)
                    textView.text = index.toString()
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)
                    builder = Notification.Builder(applicationContext, channelId)
                        .setContentTitle("${myindex-index} Client yet to your turn")
                        .setContentText("client numbre : ${index}")
                        .setSmallIcon(R.drawable.ss)
                        .setLargeIcon(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ss))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                } else {

                    builder = Notification.Builder(applicationContext)
                        .setContentTitle("textTitle")
                        .setContentText("textContent")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setLargeIcon(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ic_launcher_background))
                }
                notificationManager.notify(1234, builder.build())
                //num.text = i.toString()
            }

            override fun connectionLost(cause: Throwable?) {
                Toast.makeText(applicationContext, "Connection lost ${cause.toString()}", Toast.LENGTH_LONG).show()
                connect(applicationContext)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions()
        options.setUserName("safta")
        options.setPassword("safta023".toCharArray())
        options.setAutomaticReconnect(true)
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Toast.makeText(applicationContext, "Connection success", Toast.LENGTH_LONG).show()
                    subscribe(date)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Toast.makeText(applicationContext, "Connection failure", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Toast.makeText(applicationContext, "Subscribed to $topic", Toast.LENGTH_LONG).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Toast.makeText(applicationContext, "Failed to subscribe $topic", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Toast.makeText(applicationContext, "$msg published to $topic", Toast.LENGTH_LONG).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Toast.makeText(applicationContext, "Failed to publish $msg to $topic", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}