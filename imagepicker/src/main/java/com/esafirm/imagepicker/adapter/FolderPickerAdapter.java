package com.esafirm.imagepicker.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.features.imageloader.ImageLoader;
import com.esafirm.imagepicker.features.imageloader.ImageType;
import com.esafirm.imagepicker.listeners.OnFolderClickListener;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FolderPickerAdapter extends BaseListAdapter<FolderPickerAdapter.FolderViewHolder> {

    private final OnFolderClickListener folderClickListener;

    private final List<Folder> folders = new ArrayList<>();

    public FolderPickerAdapter(Context context, ImageLoader imageLoader, OnFolderClickListener folderClickListener) {
        super(context, imageLoader);
        this.folderClickListener = folderClickListener;
    }

    @Override
    public FolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FolderViewHolder(
                getInflater().inflate(R.layout.ef_imagepicker_item_folder, parent, false));
    }

    @Override
    public void onBindViewHolder(final FolderViewHolder holder, int position) {
        final Folder folder = folders.get(position);

        getImageLoader().loadImage(
                folder.getImages().get(0).getPath(),
                holder.image,
                ImageType.FOLDER
        );

        holder.name.setText(folders.get(position).getFolderName());
        holder.number.setText(String.valueOf(folders.get(position).getImages().size()));

        holder.itemView.setOnClickListener(v -> {
            if (folderClickListener != null)
                folderClickListener.onFolderClick(folder);
        });
    }

    public void setData(List<Folder> folders) {
        this.folders.clear();
        if (folders != null) {
            Collections.sort(folders, (o1, o2) -> {
                ArrayList<Image> leftImages = o1.getImages();
                int leftSize = leftImages != null ? leftImages.size() : 0;

                ArrayList<Image> rightImages = o2.getImages();
                int rightSize = rightImages != null ? rightImages.size() : 0;

                return rightSize - leftSize;
            });
            //保证Camera在第一位，因为刚拍的照片放Camera
            ArrayList<Folder> newList = new ArrayList<>();
            Folder camera = null;
            for (Folder f : folders) {
                if ("Camera".equals(f.getFolderName())){
                    camera = f;
                } else {
                    newList.add(f);
                }
            }
            if (camera != null) {
                newList.add(0, camera);
            }

            this.folders.addAll(newList);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private TextView name;
        private TextView number;

        FolderViewHolder(View itemView) {
            super(itemView);

            image = (ImageView) itemView.findViewById(R.id.image);
            name = (TextView) itemView.findViewById(R.id.tv_name);
            number = (TextView) itemView.findViewById(R.id.tv_number);
        }
    }
}
