package org.example;

import java.awt.Image;

public interface ClothesCallback {
    void onSelectSuccess(Image selectedClothes, String selectInfo);

    void onSelectFailure(String errorMsg);

    default void onSelectCanceled() {
    }
}