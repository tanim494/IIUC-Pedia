package com.tanim.ccepedia;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class FacultyAdapter extends RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder> {

    private final Context context;
    private final List<FacultyModel> facultyList;
    private final List<FacultyModel> fullFacultyList;

    public FacultyAdapter(Context context, List<FacultyModel> facultyList) {
        this.context = context;
        this.facultyList = facultyList;
        this.fullFacultyList = new ArrayList<>();
    }

    public void setFullList(List<FacultyModel> fullList) {
        this.fullFacultyList.clear();
        this.fullFacultyList.addAll(fullList);
    }

    public void filterList(String query) {
        String lowerCaseQuery = query.toLowerCase();
        facultyList.clear();

        if (lowerCaseQuery.isEmpty()) {
            facultyList.addAll(fullFacultyList);
        } else {
            for (FacultyModel faculty : fullFacultyList) {
                if (faculty.getName() != null && faculty.getName().toLowerCase().contains(lowerCaseQuery)) {
                    facultyList.add(faculty);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_faculty, parent, false);
        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacultyViewHolder holder, int position) {
        FacultyModel faculty = facultyList.get(position);
        holder.facultyName.setText(faculty.getName());
        holder.facultyDesignation.setText(faculty.getDesignation());

        if (faculty.getPhone() != null && !faculty.getPhone().isEmpty()) {
            holder.phoneTextView.setText("Phone: " + faculty.getPhone());
            holder.phoneTextView.setVisibility(View.VISIBLE);
        } else {
            holder.phoneTextView.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(faculty.getPhotoUrl())
                .into(holder.facultyPhoto);

        holder.itemView.setOnClickListener(v -> {
            if (faculty.getPhone() != null && !faculty.getPhone().isEmpty()) {
                String facultyFullName = faculty.getName();

                Toast.makeText(context, "Calling " + facultyFullName + " Sir...", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + faculty.getPhone()));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Phone number not available for " + faculty.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return facultyList.size();
    }

    public static class FacultyViewHolder extends RecyclerView.ViewHolder {
        ImageView facultyPhoto;
        TextView facultyName;
        TextView facultyDesignation;
        TextView phoneTextView;

        public FacultyViewHolder(@NonNull View itemView) {
            super(itemView);
            facultyPhoto = itemView.findViewById(R.id.facultyPhoto);
            facultyName = itemView.findViewById(R.id.facultyName);
            facultyDesignation = itemView.findViewById(R.id.facultyDesignation);
            phoneTextView = itemView.findViewById(R.id.facultyPhone);
        }
    }
}