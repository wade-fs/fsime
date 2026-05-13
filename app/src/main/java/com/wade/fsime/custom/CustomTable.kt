package com.wade.fsime.custom

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.wade.fsime.R
import com.wade.libs.BDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class CustomTable : Activity() {
    var mContext: Context? = null
    var listView: ListView? = null
    var btnSearch: Button? = null
    var btnAdd: Button? = null
    var btnEdit: Button? = null
    var btnSave: Button? = null
    var btnBack: Button? = null
    var etSearch: EditText? = null
    var etAdd: EditText? = null
    var etEdit: EditText? = null
    var editPos = 0
    var bdatabase: BDatabase? = null
    var backdir: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        setContentView(R.layout.custom_input_method)
        bdatabase = BDatabase(applicationContext)
        backdir = getExternalFilesDir(null).toString() + "/fsime"
        val bkdir = File(getExternalFilesDir(null), "fsime")
        if (!bkdir.exists()) {
            bkdir.mkdirs()
        }
        if (bkdir.exists()) {
            backdir = bkdir.absolutePath
        }
        listView = findViewById(R.id.list)
        val es : EditText = findViewById(R.id.etsearch)
        val ea:EditText = findViewById(R.id.etadd)
        val ee:EditText = findViewById(R.id.etedit)
        etEdit = ee
        val bs:Button = findViewById(R.id.btsearch)
        bs.setOnClickListener(View.OnClickListener {
            var word = es.getText().toString()
            if (word.length < 1) {
                Toast.makeText(mContext, "請提供（一個）中文字", Toast.LENGTH_LONG).show()
                return@OnClickListener
            }
            word = word.trim { it <= ' ' }.substring(0, 1)
            mAdapter!!.setItems(bdatabase!!.getCompose(word))
            editPos = 0
        })
        etSearch = es
        btnSearch = bs
        val ba:Button = findViewById(R.id.btadd)
        ba.setOnClickListener(View.OnClickListener {
            if (es.getText().toString().length == 0) {
                Toast.makeText(mContext, "請先查詢", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            mAdapter!!.addItem(ea.getText().toString())
            editPos = 0
        })
        etAdd = ea
        btnAdd = ba
        val be:Button = findViewById(R.id.btedit)
        btnEdit = be
        be.setOnClickListener(View.OnClickListener {
            if (es.getText().toString().length == 0) {
                Toast.makeText(mContext, "請先查詢", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            mAdapter!!.setItem(editPos, ee.getText().toString())
        })
        val bsave:Button = findViewById(R.id.btsave)
        btnSave = bsave
        bsave.setOnClickListener(View.OnClickListener {
            if (es.getText().toString().length == 0) {
                Toast.makeText(mContext, "請先查詢", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            bdatabase!!.saveCompose(es.getText().toString(), mAdapter!!.dataSet)
        })
        val bb:Button = findViewById(R.id.btback)
        btnBack = bb
        bb.setOnClickListener(View.OnClickListener { exportDB() })
        mAdapter = CustomAdapter(applicationContext, ArrayList())
        if (listView != null && mAdapter != null) {
            listView!!.adapter = mAdapter
            listView!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                if (es.getText().toString().length == 0) {
                    return@OnItemClickListener
                }
                ee.setText(mAdapter!!.getItem(position))
                editPos = position
            }
            listView!!.onItemLongClickListener =
                OnItemLongClickListener { parent, view, position, id ->
                    if (es.getText().toString().length == 0) {
                        return@OnItemLongClickListener true
                    }
                    mAdapter!!.removeItem(position)
                    true
                }
        }
        mAdapter!!.addItem("查詢: 在查詢欄輸入中文字後按查詢鈕")
        mAdapter!!.addItem("新增: 在新增欄填好後按新增鈕")
        mAdapter!!.addItem("編輯: 輕按項目，編輯好之後按編輯鈕")
        mAdapter!!.addItem("刪除: 長按項目可以刪除該項目")
        mAdapter!!.addItem("儲存: 確認編輯完之後按儲存")
        mAdapter!!.addItem("還原: 在儲存前，再按查詢即可還原")
    }

    private fun exportDB() {
        try {
            val bkdir = File(backdir)
            if (!bkdir.exists()) {
                return
            }
            val dbFile = File(getDatabasePath("b.db").absolutePath)
            val fis = FileInputStream(dbFile) // /data/user/0/com.wade.fsime/databases/b.db
            val outFileName = "$backdir/b.db"
            // Open the empty db as the output stream
            val output: OutputStream =
                FileOutputStream(outFileName) // /storage/emulated/0/Android/data/com.wade.fsime/files/fsime/b.db

            // Transfer bytes from the inputfile to the outputfile
            val buffer = ByteArray(1024000)
            var length: Int
            while (fis.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
            // Close the streams
            output.flush()
            output.close()
            fis.close()
            Toast.makeText(mContext, "備份至 $backdir/b.db", Toast.LENGTH_LONG).show()
        } catch (e: FileNotFoundException) {
        } catch (e: IOException) {
        }
    }

    companion object {
        private var mAdapter: CustomAdapter? = null
    }
}