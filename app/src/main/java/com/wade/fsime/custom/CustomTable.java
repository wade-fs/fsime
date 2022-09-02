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

    ArrayList<DataModel> dataModels;
    ListView listView;
    Button btnSearch, btnAddCompose;
    EditText etWord, etCompose;
    private static CustomAdapter adapter;
    private void Logi(String msg) {
        Log.i("FSIME", msg);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_input_method);

        listView = findViewById(R.id.list);
        etWord = findViewById(R.id.word);
        etCompose = findViewById(R.id.compose);
        btnSearch = findViewById(R.id.id_search);
        btnAddCompose = findViewById(R.id.add_compose);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logi("Search "+etWord.getText().toString());
            }
        });
        btnAddCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logi("Add compose "+etCompose.getText().toString());
            }
        });

        dataModels= new ArrayList<>();

//        dataModels.add(new DataModel("tt"));
        adapter= new CustomAdapter(getApplicationContext(), dataModels);
        if (listView != null && adapter != null) {
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    DataModel dataModel = dataModels.get(position);
                }
            });
        }
    }
}
