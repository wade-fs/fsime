package com.wade.fsime.custom;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toolbar;

import com.wade.fsime.R;

import java.util.ArrayList;

public class CustomTable extends Activity {
    ListView listView;
    Button btnSearch, btnAddCompose, btnEditCompose;
    EditText etWord, etCompose, etEdit;
    private static CustomAdapter mAdapter;
    private void Logi(String msg) {
        Log.i("FSIME", msg);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_input_method);

        listView = findViewById(R.id.list);
        etWord = findViewById(R.id.word);
        etCompose = findViewById(R.id.compose_to_add);
        etEdit = findViewById(R.id.compose_to_edit);
        btnSearch = findViewById(R.id.id_search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logi("Search "+etWord.getText().toString());
            }
        });
        btnAddCompose = findViewById(R.id.add_compose);
        btnAddCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logi("Add compose "+etCompose.getText().toString());
            }
        });
        btnEditCompose = findViewById(R.id.edit_compose);
        btnEditCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logi("Edit compose "+etCompose.getText().toString());
            }
        });

        mAdapter= new CustomAdapter(getApplicationContext(), new ArrayList<>());
        mAdapter.addItem("tt"); // TODO

        if (listView != null && mAdapter != null) {
            listView.setAdapter(mAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Logi("onClick "+mAdapter.getItem(position));
                }
            });
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    Logi("remove item "+mAdapter.getItem(position));
                    return true;
                }
            });
        }
    }
}
