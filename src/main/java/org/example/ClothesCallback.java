package org.example;

import java.awt.Image;

// 与出程序通信, 返回结果用
public interface ClothesCallback {
    void onSelectSuccess(Image selectedClothes, String selectInfo);

    void onSelectFailure(String errorMsg);

    default void onSelectCanceled() {
    }
}