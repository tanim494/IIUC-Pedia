package com.tanim.ccepedia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private final List<Course> displayList;
    private final List<Course> fullList;
    private final OnCourseClickListener clickListener;

    public interface OnCourseClickListener {
        void onCourseClick(String courseId);
    }

    public CourseAdapter(List<Course> initialCourses, OnCourseClickListener clickListener) {
        this.fullList = new ArrayList<>(initialCourses);
        this.displayList = new ArrayList<>(initialCourses);
        this.clickListener = clickListener;
    }

    public void updateData(List<Course> newCourses) {
        this.fullList.clear();
        this.fullList.addAll(newCourses);
        filterList("");
    }

    public void filterList(String query) {
        displayList.clear();
        if (query.isEmpty()) {
            displayList.addAll(fullList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Course course : fullList) {
                if (course.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    displayList.add(course);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course_card, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = displayList.get(position);
        holder.tvTitle.setText(course.getTitle());

        holder.itemView.setOnClickListener(v -> clickListener.onCourseClick(course.getId()));
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_course_title);
        }
    }
}