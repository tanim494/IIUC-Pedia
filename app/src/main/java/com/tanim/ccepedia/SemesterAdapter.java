package com.tanim.ccepedia;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class SemesterAdapter extends RecyclerView.Adapter<SemesterAdapter.SemesterViewHolder> {

    private final List<Semester> semesterList;
    private final OnSemesterClickListener clickListener;
    private Context context;

    public interface OnSemesterClickListener {
        void onSemesterClick(String semesterId);
    }

    public SemesterAdapter(List<Semester> semesterList, OnSemesterClickListener clickListener) {
        this.semesterList = semesterList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public SemesterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_semester_card, parent, false);
        return new SemesterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SemesterViewHolder holder, int position) {
        Semester semester = semesterList.get(position);
        holder.bind(semester, clickListener);
    }

    @Override
    public int getItemCount() {
        return semesterList.size();
    }

    public class SemesterViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvNumber;
        TextView tvStatusTag;
        TextView tvTitle;

        public SemesterViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardSemester);
            tvNumber = itemView.findViewById(R.id.tv_semester_number);
            tvStatusTag = itemView.findViewById(R.id.tv_status_tag);
            tvTitle = itemView.findViewById(R.id.tv_semester_title);
        }

        public void bind(Semester semester, OnSemesterClickListener listener) {
            tvNumber.setText(semester.getNumber());
            tvStatusTag.setText(semester.getStatus());
            tvTitle.setText(semester.getTitle());

            cardView.setOnClickListener(v -> listener.onSemesterClick(semester.getId()));

            applyStatusColors(semester.getStatus());
        }

        private void applyStatusColors(String status) {
            int numberBgColor;
            int tagTextColor;
            int tagBgColor;

            switch (status) {
                case "Completed":
                    numberBgColor = Color.parseColor("#4ade80");
                    tagTextColor = Color.parseColor("#15803d");
                    tagBgColor = Color.parseColor("#dcfce7");
                    break;
                case "Current":
                    numberBgColor = Color.parseColor("#3b82f6");
                    tagTextColor = Color.parseColor("#2d69eb");
                    tagBgColor = Color.parseColor("#dbeafe");
                    break;
                case "Upcoming":
                default:
                    numberBgColor = Color.parseColor("#9ca3af");
                    tagTextColor = Color.parseColor("#4b5563");
                    tagBgColor = Color.parseColor("#f3f4f6");
                    break;
            }

            tvNumber.getBackground().setTint(numberBgColor);
            tvStatusTag.setTextColor(tagTextColor);
            tvStatusTag.getBackground().setTint(tagBgColor);
        }
    }
}