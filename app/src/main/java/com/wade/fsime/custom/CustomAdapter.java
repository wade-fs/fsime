// 這邊盡量不做 UI 處理，把事件都放在 View 中
package com.wade.fsime.custom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wade.fsime.R;

import java.util.ArrayList;
import java.util.ListIterator;

public class CustomAdapter extends ArrayAdapter<String> {
    private ArrayList<String> dataSet;

    public CustomAdapter(@NonNull Context context, ArrayList<String> data) {
        super(context, R.layout.input_table_item, data);
        this.dataSet = data;
    }

    public void addItem(String item){
        dataSet.add(item);
        this.notifyDataSetChanged();
    }

    public void removeItem(int position){
        dataSet.remove(position);
        this.notifyDataSetChanged();
    }
    public void clean() {
        ListIterator<String> listIterator = dataSet.listIterator();
        while (listIterator.hasNext()) {
            listIterator.next();
            listIterator.remove();
        }
    }
    public void setItems(ArrayList<String> composes) {
        clean();
        dataSet = composes;
        this.notifyDataSetChanged();
    }

    public void setItem(int pos, String item) {
        dataSet.set(pos, item);
    }
    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public String getItem(int position) {
        return dataSet.get(position);
    }
    public ArrayList<String> getDataSet() {
        return dataSet;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView compose_item;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View currentItemView = convertView;
        ViewHolder viewHolder; // view lookup cache stored in tag

        if (currentItemView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            currentItemView = inflater.inflate(R.layout.input_table_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.compose_item   = currentItemView.findViewById(R.id.compose_item);
            currentItemView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) currentItemView.getTag();
        }
        viewHolder.compose_item.setText(getItem(position));

        return currentItemView;
    }
}
