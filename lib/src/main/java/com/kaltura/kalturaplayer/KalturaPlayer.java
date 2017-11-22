package com.kaltura.kalturaplayer;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.ads.AdController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class KalturaPlayer {
    private final Context context;
    final int partnerId;
    
    String ks;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    Player player;
    
    // Options
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private View view;
    private PKMediaEntry mediaEntry;

    // Final options
    PKMediaFormat preferredFormat;
    final String serverUrl;
    final String referrer;

    
    public KalturaPlayer(Context context, int partnerId, String ks, PKPluginConfigs pluginConfigs, Options options) {

        this.context = context;
        this.partnerId = partnerId;
        this.ks = ks;

        if (options == null) {
            options = new Options();
        }

        String serverUrl = options.serverUrl;
        if (serverUrl != null) {
            this.serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        } else {
            this.serverUrl = getDefaultServerUrl();
        }
        

        this.preload = options.preload || options.autoPlay; // autoPlay implies preload
        this.autoPlay = options.autoPlay;

        this.preferredFormat = options.preferredFormat;
        this.referrer = buildReferrer(context, options.referrer);

        initializeBackendComponents();


        registerPlugins(context);

        loadPlayer(pluginConfigs);
    }

    abstract void initializeBackendComponents();

    abstract void registerPlugins(Context context);

    abstract String getDefaultServerUrl();
    
    private static String buildReferrer(Context context, String referrer) {
        if (referrer == null) {
            referrer = context.getPackageName();
        }

        // If referrer does not have a scheme, add 'app' as scheme.
        Uri uri = Uri.parse(referrer);
        if (TextUtils.isEmpty(uri.getScheme())) {
            return uri.buildUpon().scheme("app").build().toString();
        } else {
            return referrer;
        }
    }
    
    private void loadPlayer(PKPluginConfigs pluginConfigs) {
        // Load a player preconfigured to use stats plugins and the playManifest adapter.

        PKPluginConfigs combined = new PKPluginConfigs();

        addKalturaPluginConfigs(combined);

        // Copy application-provided configs.
        if (pluginConfigs != null) {
            for (Map.Entry<String, Object> entry : pluginConfigs) {
                combined.setPluginConfig(entry.getKey(), entry.getValue());
            }
        }

        player = PlayKitManager.loadPlayer(context, combined);

        PlayManifestRequestAdapter.install(player, referrer);
    }

    abstract void addKalturaPluginConfigs(PKPluginConfigs combined);


    public View getView() {

        if (this.view != null) {
            return view;
            
        } else {
            FrameLayout view = new FrameLayout(context);
            view.setBackgroundColor(Color.BLACK);
            view.addView(player.getView(), FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

            PlaybackControlsView controlsView = new PlaybackControlsView(context);

            final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.BOTTOM | Gravity.START);
            view.addView(controlsView, layoutParams);

            controlsView.setPlayer(this);

            this.view = view;
        }

        return view;
    }

    private void maybeRemoveUnpreferredFormats(PKMediaEntry entry) {
        if (preferredFormat == null) {
            return;
        }
        
        List<PKMediaSource> preferredSources = new ArrayList<>(1);
        for (PKMediaSource source : entry.getSources()) {
            if (source.getMediaFormat() == preferredFormat) {
                preferredSources.add(source);
            }
        }
        
        if (!preferredSources.isEmpty()) {
            entry.setSources(preferredSources);
        }
        
        // otherwise, leave the original source list.
    }

    public void setMedia(PKMediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        
        if (preload) {
            prepare();
        }
    }

    public void prepare() {
        final PKMediaConfig config = new PKMediaConfig()
                .setMediaEntry(mediaEntry)
                .setStartPosition((long) (startPosition * 1000));

        player.prepare(config);

        if (autoPlay) {
            player.play();
        }
    }

    public String getKs() {
        return ks;
    }

    public void setKs(String ks) {
        this.ks = ks;
        updateKs(ks);
    }

    abstract void updateKs(String ks);

    // Player controls
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        player.updatePluginConfig(pluginName, pluginConfig);
    }

    public void onApplicationPaused() {
        player.onApplicationPaused();
    }

    public void onApplicationResumed() {
        player.onApplicationResumed();
    }

    public void destroy() {
        player.destroy();
    }

    public void stop() {
        player.stop();
    }

    public void play() {
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void replay() {
        player.replay();
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    public void setVolume(float volume) {
        player.setVolume(volume);
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        player.addEventListener(listener, events);
    }
    
    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        player.addStateChangeListener(listener);
    }

    public void changeTrack(String uniqueId) {
        player.changeTrack(uniqueId);
    }

    public void seekTo(long position) {
        player.seekTo(position);
    }

    public AdController getAdController() {
        return player.getAdController();
    }

    public String getSessionId() {
        return player.getSessionId();
    }

    public boolean isLiveStream() {
        return player.isLiveStream();
    }

    public double getStartPosition() {
        return startPosition;
    }

    public KalturaPlayer setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public boolean isPreload() {
        return preload;
    }

    public KalturaPlayer setPreload(boolean preload) {
        this.preload = preload;
        return this;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public KalturaPlayer setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    public PKMediaFormat getPreferredFormat() {
        return preferredFormat;
    }

    public KalturaPlayer setPreferredFormat(PKMediaFormat preferredFormat) {
        this.preferredFormat = preferredFormat;
        return this;
    }

    void mediaLoadCompleted(final ResultElement<PKMediaEntry> response, final OnEntryLoadListener onEntryLoadListener) {
        final PKMediaEntry entry = response.getResponse();

        maybeRemoveUnpreferredFormats(entry);

        mediaEntry = entry;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onEntryLoadListener.onMediaEntryLoaded(entry, response.getError());
                if (preload && response.isSuccess()) {
                    prepare();
                }
            }
        });
    }

    public interface OnEntryLoadListener {
        void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error);
    }

    public static class Options {
        public boolean autoPlay;
        public boolean preload;
        public PKMediaFormat preferredFormat;
        public String serverUrl;
        public String referrer;

        public Options(boolean autoPlay, boolean preload) {
            this(autoPlay, preload, null);
        }

        public Options(boolean autoPlay, boolean preload, PKMediaFormat preferredFormat) {
            this(autoPlay, preload, preferredFormat, null);
        }

        public Options(boolean autoPlay, boolean preload, PKMediaFormat preferredFormat,
                       String referrer) {
            this(autoPlay, preload, preferredFormat, referrer, null);
        }

        public Options(boolean autoPlay, boolean preload, PKMediaFormat preferredFormat,
                       String referrer, String serverUrl) {
            
            this.autoPlay = autoPlay;
            this.preload = preload;
            this.preferredFormat = preferredFormat;
            this.serverUrl = serverUrl;
            this.referrer = referrer;
        }
        
        public static Options autoPlay() {
            return new Options(true, true, null, null, null);
        }

        public Options() {}
    }
}
