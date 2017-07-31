package com.esafirm.imagepicker.features.camera;

import android.content.Context;
import android.content.Intent;

import com.esafirm.imagepicker.features.ImagePickerConfig;

import java.io.Serializable;

public interface CameraModule extends Serializable {
    Intent getCameraIntent(Context context, ImagePickerConfig config);

    void getImage(Context context, Intent intent, OnImageReadyListener imageReadyListener);
}
