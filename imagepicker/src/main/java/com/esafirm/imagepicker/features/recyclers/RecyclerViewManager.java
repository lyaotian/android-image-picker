package com.esafirm.imagepicker.features.recyclers;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.adapter.FolderPickerAdapter;
import com.esafirm.imagepicker.adapter.ImagePickerAdapter;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.ImagePickerConfig;
import com.esafirm.imagepicker.features.imageloader.ImageLoader;
import com.esafirm.imagepicker.listeners.OnFolderClickListener;
import com.esafirm.imagepicker.listeners.OnImageClickListener;
import com.esafirm.imagepicker.listeners.OnImageSelectedListener;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;
import com.esafirm.imagepicker.view.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;

import static com.esafirm.imagepicker.features.ImagePicker.MODE_MULTIPLE;
import static com.esafirm.imagepicker.features.ImagePicker.MODE_SINGLE;

public class RecyclerViewManager {
    private static final String KEY_STATE = "state_recycler_view";

    private final Context context;
    private final RecyclerView recyclerView;
    private final ImagePickerConfig config;

    private GridLayoutManager layoutManager;
    private GridSpacingItemDecoration itemOffsetDecoration;

    private ImagePickerAdapter imageAdapter;
    private FolderPickerAdapter folderAdapter;

    private Parcelable foldersState;

    private int imageColumns;
    private int folderColumns;

    public RecyclerViewManager(RecyclerView recyclerView, ImagePickerConfig config, int orientation) {
        this.recyclerView = recyclerView;
        this.config = config;
        this.context = recyclerView.getContext();
        changeOrientation(orientation);
    }

    /**
     * Set item size, column size base on the screen orientation
     */
    public void changeOrientation(int orientation) {
        int baseValue = config.getColumn();
        int value;
        if (baseValue > 0) {
            value = baseValue;
        } else {
            value = 3;
        }
        imageColumns = orientation == Configuration.ORIENTATION_PORTRAIT ? value : value + 2;
        folderColumns = orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4;

        boolean shouldShowFolder = config.isFolderMode() && isDisplayingFolderView();
        int columns = shouldShowFolder ? folderColumns : imageColumns;
        layoutManager = new GridLayoutManager(context, columns);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        setItemDecoration(columns);
    }

    public void setupAdapters(OnImageClickListener onImageClickListener, OnFolderClickListener onFolderClickListener) {
        ArrayList<Image> selectedImages = null;
        if (config.getMode() == MODE_MULTIPLE && !config.getSelectedImages().isEmpty()) {
            selectedImages = config.getSelectedImages();
        }

        /* Init folder and image adapter */
        final ImageLoader imageLoader = config.getImageLoader();
        imageAdapter = new ImagePickerAdapter(context, imageLoader, selectedImages, onImageClickListener);
        folderAdapter = new FolderPickerAdapter(context, imageLoader, bucket -> {
            foldersState = recyclerView.getLayoutManager().onSaveInstanceState();
            onFolderClickListener.onFolderClick(bucket);
        });
    }

    private void setItemDecoration(int columns) {
        if (itemOffsetDecoration != null) {
            recyclerView.removeItemDecoration(itemOffsetDecoration);
        }
        itemOffsetDecoration = new GridSpacingItemDecoration(
                columns,
                context.getResources().getDimensionPixelSize(R.dimen.ef_item_padding),
                false
        );
        recyclerView.addItemDecoration(itemOffsetDecoration);

        layoutManager.setSpanCount(columns);
    }

    public void handleBack(OnBackAction action) {
        if (config.isFolderMode() && !isDisplayingFolderView()) {
            setFolderAdapter(null);
            action.onBackToFolder();
            return;
        }
        action.onFinishImagePicker();
    }

    private boolean isDisplayingFolderView() {
        return recyclerView.getAdapter() == null || recyclerView.getAdapter() instanceof FolderPickerAdapter;
    }

    public String getTitle() {
        int imageSize = imageAdapter.getSelectedImages().size();
        if (imageSize <= 0 && isDisplayingFolderView()) {
            return config.getFolderTitle();
        }

        if (config.getMode() == ImagePicker.MODE_MULTIPLE) {
            return config.getLimit() == ImagePicker.MAX_LIMIT
                    ? String.format(context.getString(R.string.ef_selected), imageSize)
                    : String.format(context.getString(R.string.ef_selected_with_limit), imageSize, config.getLimit());
        }

        return config.getImageTitle();
    }

    public void setImageAdapter(List<Image> images) {
        imageAdapter.setData(images);
        setItemDecoration(imageColumns);
        recyclerView.setAdapter(imageAdapter);
    }

    public void setFolderAdapter(List<Folder> folders) {
        if (folders == null) {
            folderAdapter.notifyDataSetChanged();
        }else{
            folderAdapter.setData(folders);
        }
        setItemDecoration(folderColumns);
        recyclerView.setAdapter(folderAdapter);

        if (foldersState != null) {
            layoutManager.setSpanCount(folderColumns);
            recyclerView.getLayoutManager().onRestoreInstanceState(foldersState);
        }
    }

    /* --------------------------------------------------- */
    /* > Images */
    /* --------------------------------------------------- */

    private void checkAdapterIsInitialized() {
        if (imageAdapter == null) {
            throw new IllegalStateException("Must call setupAdapters first!");
        }
    }

    public List<Image> getSelectedImages() {
        checkAdapterIsInitialized();
        return imageAdapter.getSelectedImages();
    }

    public void setImageSelectedListener(OnImageSelectedListener listener) {
        checkAdapterIsInitialized();
        imageAdapter.setImageSelectedListener(listener);
    }

    public boolean selectImage(boolean isSelected) {
        if (config.getMode() == ImagePicker.MODE_MULTIPLE) {
            if (isSelected) {
                if (imageAdapter.getSelectedImages().size() >= config.getLimit()) {
                    Toast.makeText(context, R.string.ef_msg_limit_images, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        } else if (config.getMode() == ImagePicker.MODE_SINGLE) {
            if (imageAdapter.getSelectedImages().size() > 0) {
                imageAdapter.removeAllSelectedSingleClick();
            }
        }
        return true;
    }

    public boolean isShowDoneButton() {
        return !isDisplayingFolderView()
                && !imageAdapter.getSelectedImages().isEmpty()
                && !(config.getMode() == MODE_SINGLE && config.isReturnAfterFirst());
    }

    public void onSaveState(Bundle outputState) {
        if (outputState == null) {
            return;
        }

        outputState.putParcelableArrayList(KEY_STATE, new ArrayList<>(imageAdapter.getSelectedImages()));
    }

    public void onRestoreState(Bundle inputState) {
        if (inputState == null) {
            return;
        }

        ArrayList<Image> selectedList = inputState.getParcelableArrayList(KEY_STATE);
        imageAdapter.setSelectedImages(selectedList);
    }
}
