package com.kaltura.tvplayer.playlist;

import androidx.annotation.Nullable;

public abstract class PlaylistOptions {
    public boolean loopEnabled;
    public boolean autoContinue = true;
    public boolean recoverOnError = false;
    public int startIndex = 0;
    public CountDownOptions playlistCountDownOptions = new CountDownOptions();
}

