package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class DriveLinkAdapter extends RecyclerView.Adapter<DriveLinkAdapter.ViewHolder> {

    private List<DriveLink> driveLinks;
    private List<DriveLink> fullLinkList;

    public DriveLinkAdapter(List<DriveLink> driveLinks) {
        this.driveLinks = driveLinks;
        this.fullLinkList = new ArrayList<>();
    }

    public void setFullList(List<DriveLink> fullList) {
        this.fullLinkList.clear();
        this.fullLinkList.addAll(fullList);
    }

    public void filterList(String query) {
        List<DriveLink> filteredList = new ArrayList<>();

        if (fullLinkList != null) {
            String lowerCaseQuery = query.toLowerCase();
            for (DriveLink link : fullLinkList) {
                if (link.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(link);
                }
            }
        }

        driveLinks.clear();
        driveLinks.addAll(filteredList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        MaterialCardView cardView;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.linkTitle);
            cardView = view.findViewById(R.id.linkCardView);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_link, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DriveLink link = driveLinks.get(position);
        holder.title.setText(link.getTitle());

        holder.itemView.setOnClickListener(v -> {
            String url = link.getUrl();
            if (url != null && !url.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Could not open link", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(v.getContext(), "Link URL is missing", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return driveLinks.size();
    }
}