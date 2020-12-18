package com.yuwee.yuweesdkdemo.model;

public enum ViewType {
    MY_MESSAGE(0),
    OTHERS_MESSAGE(1),
    CALL(2),
    MY_FILE(3),
    OTHER_FILE(4);

    public int type;

    ViewType(int i) {
        type = i;
    }
}
