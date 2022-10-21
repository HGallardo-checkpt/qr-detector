package com.checkpoint.qr.detector.reader.dto;


import com.google.gson.annotations.SerializedName;

public class QRDetectorReaderDTO {
    @SerializedName("translation")
    private final String mTranslation;
    @SerializedName("direction")
    private final String mDirection;


    public QRDetectorReaderDTO(final String translation,
                        final String direction) {

        mTranslation = translation;
        mDirection = direction;
    }
}
