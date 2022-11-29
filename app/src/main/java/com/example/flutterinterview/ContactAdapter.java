package com.example.flutterinterview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.MonitorAdvisorViewHolder> implements Filterable {

    Context c;
    public ArrayList<Users> mMonitorUserList, filterList;
    public ContactAdapter.OnItemClickListener mListener;
    //SearchFilter myFilter;
    static final String TAG = "FI-ContactAdapter";


    @Override
    public Filter getFilter() {
        return exampleFilter;
    }

    //Search filter
    Filter exampleFilter = new Filter(){
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            ArrayList<Users> filterResults = new ArrayList<>();

            Log.d(TAG, "performFiltering: No Strings " + constraint + " Length " + constraint.length());

            if(constraint != null && constraint.length()>0){
                String searchStr = constraint.toString().toLowerCase().trim();
                for(Users filterSearchList : mMonitorUserList){
                    Log.d(TAG, "performFiltering: Hello?");
                    if(filterSearchList.getFirst_name().toLowerCase().contains(searchStr) || filterSearchList.getLast_name().toLowerCase().contains(searchStr)){
                        filterResults.add(filterSearchList);
                        Log.d(TAG, "performFiltering: test?");

                    }
                }
                results.count = filterList.size();
                results.values = filterResults;
            } else if (constraint == null || constraint.length() == 0){
                filterResults.addAll(filterList);
                results.count = filterResults.size();
                results.values = filterResults;
                Log.d(TAG, "performFiltering: filterResults " +  mMonitorUserList.toString());
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults results) {
            Log.d(TAG, "publishResults: results.values " + results.values);
            mMonitorUserList.clear();
            mMonitorUserList.addAll((ArrayList<Users>) results.values);
            notifyDataSetChanged();

            Log.d(TAG, "publishResults: filterlist publish results" + filterList.toString());
        }
    };

    public void showMenu(int position) {
        for(int i=0; i<mMonitorUserList.size(); i++){
            mMonitorUserList.get(i).setShowMenu(false);
        }
        mMonitorUserList.get(position).setShowMenu(true);
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onSendEmailClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class MonitorAdvisorViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView, image_sendEmail;
        public TextView mTextView1, mTextView2;
        public CardView cardItem;

        public MonitorAdvisorViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.imageView);
            mTextView1 = itemView.findViewById(R.id.text_userName);
            mTextView2 = itemView.findViewById(R.id.text_email);
            image_sendEmail = itemView.findViewById(R.id.image_sendEmail);
            cardItem = itemView.findViewById(R.id.cv_item);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
            image_sendEmail.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onSendEmailClick(position);
                        }
                    }
                }
            });
        }
    }


    public ContactAdapter(ArrayList<Users> monitorUserList) {
        mMonitorUserList = new ArrayList<>(monitorUserList);
        filterList = monitorUserList;
    }

    @NonNull
    @Override
    public MonitorAdvisorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_contact_item, parent, false);
        MonitorAdvisorViewHolder mavh = new MonitorAdvisorViewHolder(v, mListener);
        return mavh;
    }

    @Override
    public void onBindViewHolder(@NonNull MonitorAdvisorViewHolder holder, int position) {
        Users currentItem = mMonitorUserList.get(position);
        Picasso.get().load(currentItem.getAvatar()).into(holder.mImageView);
        holder.mTextView1.setText(currentItem.getFirst_name() + " " + currentItem.getLast_name());
        holder.mTextView2.setText(currentItem.getEmail());

    }

    @Override
    public int getItemCount() {
        return mMonitorUserList.size();
    }



}
