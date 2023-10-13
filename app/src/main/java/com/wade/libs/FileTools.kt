package com.wade.libs

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * FileTools fileTools = new FileTools(Environment.getExternalStorageDirectory()+"/Status.txt");
 * fileTools.append("PWR_ON,"+ Calendar.getInstance(timeZone).getTime().toString()+"\n");
 */
class FileTools {
    private var fileName: String

    constructor(fn: String) {
        fileName = fn
    }

    constructor() {
        fileName = ""
    }

    fun setFileName(fn: String) {
        fileName = fn
    }

    fun load(): String {
        val file = File(fileName)
        if (!file.exists()) return ""
        try {
            val f = FileReader(file)
            val bufferedReader = BufferedReader(f)
            var text = ""
            var buf = ""
            while (bufferedReader.readLine().also { buf = it } != null) {
                text += """
                    $buf
                    
                    """.trimIndent()
            }
            bufferedReader.close()
            f.close()
            return text
        } catch (e: IOException) {
            Log.d("MyLog", "'" + fileName + "' 讀檔錯誤: " + e.message)
        }
        return ""
    }

    fun delete() {
        val file = File(fileName)
        file.delete()
    }

    private fun write(buf: String, append: Boolean) {
        try {
            val file = File(fileName)
            val f = FileWriter(file, append)
            val bufferedWriter = BufferedWriter(f)
            bufferedWriter.write(buf)
            bufferedWriter.flush()
            bufferedWriter.close()
            f.close()
        } catch (e: IOException) {
            Log.d("MyLog", "'" + fileName + "' 寫檔錯誤: " + e.message)
        }
    }

    fun save(buf: String) {
        write(buf, false)
    }

    fun append(buf: String) {
        write(buf, true)
        Log.d("MyLog", "append: '" + buf.trim { it <= ' ' } + "'")
    }

    fun empty() {
        write("", false)
    }
}