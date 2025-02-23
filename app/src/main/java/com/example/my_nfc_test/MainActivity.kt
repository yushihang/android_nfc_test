package com.example.my_nfc_test

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
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
        // 初始化 NFC 适配器
        adapter = NfcAdapter.getDefaultAdapter(this)
        
        // 处理启动 Intent
        if (intent != null) {
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

        // put your code here...
    }
    
    private fun processIntent(intent: Intent) {
        // 尝试从 intent 获取 tag
        var tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        
        // 如果 intent 中没有 tag，尝试主动获取当前的 tag
        if (tag == null && adapter?.isEnabled == true) {
            try {
                // 注意：这种方式并不能保证一定能读到卡，因为 NFC 读取是基于事件的
                // 只有当卡片在读取范围内时才能读取
                val tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                if (tagFromIntent != null) {
                    tag = tagFromIntent
                }
            } catch (e: Exception) {
                // 处理异常
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("NFC 读取失败")
                    .setMessage("请确保 NFC 卡片已贴近设备")
                    .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                    .show()
                return
            }
        }

        if (tag == null) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("未检测到 NFC")
                .setMessage("请将 NFC 卡片贴近设备背面")
                .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        // 原有的读卡逻辑
        val message = buildString {
            append("Intent Action: ${intent.action}\n")
            append("Tag ID: ${tag.id?.joinToString(":") { "%02X".format(it) }}\n\n")
            
            // 读取 IsoDep 数据
            android.nfc.tech.IsoDep.get(tag)?.let { isoDep ->
                try {
                    isoDep.connect()
                    // 发送 SELECT APDU 命令
                    val command = byteArrayOf(
                        0x00.toByte(),  // CLA
                        0xA4.toByte(),  // INS
                        0x04.toByte(),  // P1
                        0x00.toByte(),  // P2
                        0x07.toByte(),  // Lc
                        0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(), 
                        0x47.toByte(), 0x10.toByte(), 0x01.toByte()  // AID
                    )
                    val response = isoDep.transceive(command)
                    append("IsoDep 响应: ${bytesToHex(response)}\n")
                    isoDep.close()
                } catch (e: Exception) {
                    append("IsoDep 读取失败: ${e.message}\n")
                }
            }
            
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
            }
            
            // 读取 MifareClassic 数据
            android.nfc.tech.MifareClassic.get(tag)?.let { mifare ->
                try {
                    mifare.connect()
                    append("Mifare Classic:\n")
                    append("- 类型: ${mifare.type}\n")
                    append("- 扇区数: ${mifare.sectorCount}\n")
                    append("- 块数: ${mifare.blockCount}\n")
                    mifare.close()
                } catch (e: Exception) {
                    append("Mifare 读取失败: ${e.message}\n")
                }
            }
        }

        // 显示读取结果
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("NFC 卡片内容")
            .setMessage(message)
            .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }
}