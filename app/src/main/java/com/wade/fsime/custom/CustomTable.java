package com.wade.fsime.custom;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import com.wade.fsime.R;
import com.wade.fsime.database.BDatabase;

public class CustomTable extends Activity {
    Context mContext;
    ListView listView;
    Button btnSearch, btnAdd, btnEdit, btnSave, btnBack;
    EditText etSearch, etAdd, etEdit;
    private static CustomAdapter mAdapter;
    int editPos = 0;
    BDatabase bdatabase;
    String backdir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.custom_input_method);

        bdatabase = new BDatabase(getApplicationContext());
        backdir = getExternalFilesDir(null) + "/fsime";
        File bkdir = new File(getExternalFilesDir(null), "fsime");
        if (!bkdir.exists()) {
            bkdir.mkdirs();
        }
        if (bkdir.exists()) {
            backdir = bkdir.getAbsolutePath();
        }
        listView = findViewById(R.id.list);
        etSearch = findViewById(R.id.etsearch);
        etAdd = findViewById(R.id.etadd);
        etEdit = findViewById(R.id.etedit);
        btnSearch = findViewById(R.id.btsearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String word = etSearch.getText().toString();
                if (word.length() < 1) {
                    Toast.makeText(mContext, "請提供（一個）中文字", Toast.LENGTH_LONG).show();
                    return;
                }
                word = word.trim().substring(0,1);
                mAdapter.setItems(bdatabase.getCompose(word));
                editPos = 0;
            }
        });
        btnAdd = findViewById(R.id.btadd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etSearch.getText().toString().length() == 0) {
                    Toast.makeText(mContext, "請先查詢", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAdapter.addItem(etAdd.getText().toString());
                editPos = 0;
            }
        });
        btnEdit = findViewById(R.id.btedit);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etSearch.getText().toString().length() == 0) {
                    Toast.makeText(mContext, "請先查詢", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAdapter.setItem(editPos, etEdit.getText().toString());
            }
        });
        btnSave = findViewById(R.id.btsave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etSearch.getText().toString().length() == 0) {
                    Toast.makeText(mContext, "請先查詢", Toast.LENGTH_SHORT).show();
                    return;
                }
                bdatabase.saveCompose(etSearch.getText().toString(), mAdapter.getDataSet());
            }
        });
        btnBack = findViewById(R.id.btback);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportDB();
            }
        });

        mAdapter= new CustomAdapter(getApplicationContext(), new ArrayList<>());
        if (listView != null && mAdapter != null) {
            listView.setAdapter(mAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (etSearch.getText().toString().length() == 0) {
                        return;
                    }
                    etEdit.setText(mAdapter.getItem(position));
                    editPos = position;
                }
            });
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    if (etSearch.getText().toString().length() == 0) {
                        return true;
                    }
                    mAdapter.removeItem(position);
                    return true;
                }
            });
        }
        mAdapter.addItem("查詢: 在查詢欄輸入中文字後按查詢鈕");
        mAdapter.addItem("新增: 在新增欄填好後按新增鈕");
        mAdapter.addItem("編輯: 輕按項目，編輯好之後按編輯鈕");
        mAdapter.addItem("刪除: 長按項目可以刪除該項目");
        mAdapter.addItem("儲存: 確認編輯完之後按儲存");
        mAdapter.addItem("還原: 在儲存前，再按查詢即可還原");
    }

    private void exportDB() {
        try {
            File bkdir = new File(backdir);
            if (!bkdir.exists()) {
                return;
            }

            File dbFile = new File(this.getDatabasePath("b.db").getAbsolutePath());
            FileInputStream fis = new FileInputStream(dbFile); // /data/user/0/com.wade.fsime/databases/b.db

            String outFileName = backdir + "/b.db";
            // Open the empty db as the output stream
            OutputStream output = new FileOutputStream(outFileName); // /storage/emulated/0/Android/data/com.wade.fsime/files/fsime/b.db

            // Transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024000];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            // Close the streams
            output.flush();
            output.close();
            fis.close();
            Toast.makeText(mContext, "備份至 "+backdir+"/b.db", Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }
}
