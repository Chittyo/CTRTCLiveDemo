package com.example.rtcdemo.stutas;

import android.content.Context;

public interface IStatus {

    void config(Context context);

    void joinRoom(String roomId);

    void publishDefaultAVStream();

    void subscribeAVStream();

}
