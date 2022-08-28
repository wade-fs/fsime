package com.wade.fsime.custom;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wade.fsime.R;

import java.util.ArrayList;

public class CustomAdapter extends ArrayAdapter<DataModel> implements View.OnClickListener{

    private ArrayList<DataModel> dataSet;
    Context mContext;

    public CustomAdapter(@NonNull Context context, ArrayList<DataModel> data) {
        super(context, R.layout.input_table_item, data);
        this.dataSet = data;
        this.mContext=context;
    }

    // View lookup cache
    private static class ViewHolder {
        TextView compose_item;
        Button delete_compose;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View currentItemView = convertView;

        ViewHolder viewHolder; // view lookup cache stored in tag

        if (currentItemView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            currentItemView = inflater.inflate(R.layout.input_table_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.compose_item = (TextView) currentItemView.findViewById(R.id.compose_item);
            currentItemView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) currentItemView.getTag();
        }

        DataModel dataModel = getItem(position);
        viewHolder.compose_item.setText(dataModel.getComposeItem());
//        viewHolder.delete_compose.setOnClickListener(this);
//        viewHolder.delete_compose.setTag(position);

        return currentItemView;
    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        DataModel dataModel=(DataModel)object;

        switch (v.getId())
        {
            case R.id.delete_compose:
//                Snackbar.make(v, "Release date " +dataModel.getFeature(), Snackbar.LENGTH_LONG)
//                        .setAction("No action", null).show();
                break;
        }
    }

    private void Logi(String msg) {
        Log.i("FSIME", msg);
    }
}
