package com.wade.fsime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.wade.libs.BDatabase
import java.io.File

class DatabaseManagementActivity : AppCompatActivity() {

    private lateinit var db: BDatabase
    private lateinit var etMixBatch: EditText
    private lateinit var etCharToEdit: EditText
    private lateinit var etCharEngList: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_db_management)

        db = BDatabase(this)

        etMixBatch = findViewById(R.id.et_mix_batch)
        etCharToEdit = findViewById(R.id.et_char_to_edit)
        etCharEngList = findViewById(R.id.et_char_eng_list)

        setupTabs()

        findViewById<Button>(R.id.btn_export_db).setOnClickListener {
            if (checkStoragePermissions()) {
                exportDatabase()
            } else {
                requestStoragePermissions()
            }
        }

        findViewById<Button>(R.id.btn_import_db).setOnClickListener {
            if (checkStoragePermissions()) {
                importDatabase()
            } else {
                requestStoragePermissions()
            }
        }

        findViewById<Button>(R.id.btn_batch_import).setOnClickListener {
            val text = etMixBatch.text.toString()
            if (text.isNotBlank()) {
                val lines = text.lines().filter { it.isNotBlank() }
                val count = db.batchImportMix(lines)
                Toast.makeText(this, "Imported $count entries", Toast.LENGTH_SHORT).show()
                etMixBatch.setText("")
            }
        }

        findViewById<Button>(R.id.btn_search_char).setOnClickListener {
            val ch = etCharToEdit.text.toString()
            if (ch.length == 1) {
                val codes = db.reverseLookup(ch)
                etCharEngList.setText(codes.joinToString("\n"))
            }
        }

        findViewById<Button>(R.id.btn_save_char_mix).setOnClickListener {
            val ch = etCharToEdit.text.toString()
            if (ch.length == 1) {
                val text = etCharEngList.text.toString()
                val codes = text.lines().filter { it.isNotBlank() }.toCollection(ArrayList())
                db.saveCompose(ch, codes)
                Toast.makeText(this, "Saved for $ch", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val layoutBackup = findViewById<View>(R.id.layout_backup)
        val layoutBatch = findViewById<View>(R.id.layout_batch)
        val layoutEdit = findViewById<View>(R.id.layout_edit)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                layoutBackup.visibility = View.GONE
                layoutBatch.visibility = View.GONE
                layoutEdit.visibility = View.GONE

                when (tab?.position) {
                    0 -> layoutBackup.visibility = View.VISIBLE
                    1 -> layoutBatch.visibility = View.VISIBLE
                    2 -> layoutEdit.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                100
            )
        }
    }

    private fun exportDatabase() {
        try {
            db.readableDatabase
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dbFile = getDatabasePath("b.db")
        if (!dbFile.exists()) {
            val dbDir = File(applicationInfo.dataDir, "databases")
            val files = dbDir.list()?.joinToString(", ") ?: "none"
            Toast.makeText(this, "DB not found at ${dbFile.absolutePath}. Files in dir: $files", Toast.LENGTH_LONG).show()
            return
        }
        val targetFile = File(Environment.getExternalStorageDirectory(), "b.db")
        if (copyFile(dbFile, targetFile)) {
            Toast.makeText(this, getString(R.string.msg_export_success, targetFile.absolutePath), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, R.string.msg_export_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importDatabase() {
        val sourceFile = File(Environment.getExternalStorageDirectory(), "b.db")
        if (!sourceFile.exists()) {
            Toast.makeText(this, R.string.msg_import_no_file, Toast.LENGTH_SHORT).show()
            return
        }
        val dbFile = getDatabasePath("b.db")
        db.close()
        if (copyFile(sourceFile, dbFile)) {
            Toast.makeText(this, R.string.msg_import_success, Toast.LENGTH_LONG).show()
            etCharEngList.postDelayed({
                restartApp()
            }, 2000)
        } else {
            Toast.makeText(this, R.string.msg_import_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    private fun copyFile(src: File, dst: File): Boolean {
        return try {
            src.inputStream().use { input ->
                dst.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
