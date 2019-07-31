package com.kaltura.tvplayer.offline;

import android.content.Context;
import com.kaltura.playkit.*;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;

import java.io.IOException;

abstract class AbstractOfflineManager extends OfflineManager {
    final Context appContext;
    final LocalAssetsManager localAssetsManager;
    private String kalturaServerUrl;
    private Integer kalturaPartnerId;
    private DownloadProgressListener downloadProgressListener;
    private AssetStateListener assetStateListener;
    private String ks;

    AbstractOfflineManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.localAssetsManager = new LocalAssetsManager(appContext);
    }

    @Override
    public final void prepareAsset(MediaOptions mediaOptions, SelectionPrefs prefs,
                                   PrepareCallback prepareCallback) throws IllegalStateException {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(kalturaServerUrl, kalturaPartnerId, ks, null);

        mediaEntryProvider.load(response -> {
            if (response.isSuccess()) {
                prepareAsset(response.getResponse(), prefs, prepareCallback);
            } else {
                prepareCallback.onPrepareError(new IOException(response.getError().getMessage()));
            }
        });
    }

    @Override
    public final void sendAssetToPlayer(String assetId, KalturaPlayer player) {
        final PKMediaEntry entry = getLocalPlaybackEntry(assetId);
        player.setMedia(entry);
    }

    @Override
    public void setKalturaServerUrl(String url) {
        this.kalturaServerUrl = url;
    }

    @Override
    public void setKalturaPartnerId(int partnerId) {
        this.kalturaPartnerId = partnerId;
    }

    @Override
    public void setAssetStateListener(AssetStateListener listener) {
        this.assetStateListener = listener;
    }

    @Override
    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
    }

    @Override
    public void setKs(String ks) {
        this.ks = ks;
    }

}
