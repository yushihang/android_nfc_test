package com.example.my_nfc_test

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.my_nfc_test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>
    private var adapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NfcAdapter.getDefaultAdapter(this)
        
        // Fix the condition check
        if (intent?.action in arrayOf(
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED
        )) {
            processIntent(intent)
        }
        
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)  // 在 onCreate 方法中初始化

        // 修改 IntentFilter 配置
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("fail", e)
            }
        }
        val techDiscovered = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tagDiscovered = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        
        intentFiltersArray = arrayOf(ndef, techDiscovered, tagDiscovered)
        
        techListsArray = arrayOf(arrayOf(
            android.nfc.tech.IsoDep::class.java.name,
            android.nfc.tech.NfcA::class.java.name,
            android.nfc.tech.NfcB::class.java.name,
            android.nfc.tech.NfcF::class.java.name,
            android.nfc.tech.NfcV::class.java.name,
            android.nfc.tech.Ndef::class.java.name,
            android.nfc.tech.NdefFormatable::class.java.name,
            android.nfc.tech.MifareClassic::class.java.name,
            android.nfc.tech.MifareUltralight::class.java.name
        ))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    public override fun onPause() {
        super.onPause()
        adapter?.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        adapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
        // 处理当前 Intent

        // put your code here...
    }
    
    private fun processIntent(intent: Intent) {
        // 打印 intent 信息
        val intentInfo = "Action: ${intent.action}\n" +
                        "Type: ${intent.type}\n" +
                        "Data: ${intent.data}\n" +
                        "Categories: ${intent.categories}\n" +
                        "Extras: ${intent.extras?.keySet()?.joinToString()}"
        
        android.widget.Toast.makeText(this, intentInfo, android.widget.Toast.LENGTH_LONG).show()

        val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java) ?: return

        val message = buildString {
            // 读取 NDEF 数据
            android.nfc.tech.Ndef.get(tag)?.let { ndef ->
                try {
                    ndef.connect()
                    val ndefMessage = ndef.cachedNdefMessage
                    append("NDEF 内容:\n")
                    ndefMessage?.records?.forEach { record ->
                        append("- TNF: ${record.tnf}, Type: ${String(record.type)}\n")
                        append("  Payload: ${String(record.payload)}\n")
                    }
                    ndef.close()
                } catch (e: Exception) {
                    append("NDEF 读取失败: ${e.message}\n")
                }
            } ?: append("该卡片不支持 NDEF 格式\n")
        }

        showAlert("NFC 卡片内容", message)
    }

    private fun showAlert(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    

    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("NFC_DEBUG", "onNewIntent: action=${intent.action}")
        
        // Fix the condition check
        if (intent.action in arrayOf(
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED
        )) {
            processIntent(intent)
        }
    }
}