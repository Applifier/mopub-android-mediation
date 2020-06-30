package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.unityads.BuildConfig;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.headerbidding.IHeaderBiddingListener;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class UnityAdsAdapterConfiguration extends BaseAdapterConfiguration {

    // Adapter's keys
    public static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String ADAPTER_NAME = UnityAdsAdapterConfiguration.class.getSimpleName();

    private boolean requestingToken = false;
    private String token = null;

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        if (!requestingToken) {
            requestingToken = true;
            UnityAds.requestToken();
        }
        return token;
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        final String sdkVersion = UnityAds.getVersion();

        if (!TextUtils.isEmpty(sdkVersion)) {
            return sdkVersion;
        }

        final String adapterVersion = getAdapterVersion();
        return adapterVersion.substring(0, adapterVersion.lastIndexOf('.'));
    }

    @Override
    public void initializeNetwork(@NonNull final Context context, @Nullable final Map<String, String> configuration, @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        boolean networkInitializationSucceeded = false;

        UnityRouter.subscribeToListeners();

        synchronized (UnityAdsAdapterConfiguration.class) {
            try {
                if (UnityAds.isInitialized()) {
                    networkInitializationSucceeded = true;
                } else if (configuration != null && context instanceof Activity) {
                    UnityRouter.initUnityAds(configuration, (Activity) context);

                    networkInitializationSucceeded = true;
                } else {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity Ads initialization not started. " +
                            "Context is not an Activity. Note that initialization on the first app launch is a no-op.");
                }
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Unity Ads has encountered " +
                        "an exception.", e);
            }
        }
        if (networkInitializationSucceeded) {
            listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        } else {
            listener.onNetworkInitializationFinished(UnityAdsAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }

        MoPubLog.LogLevel logLevel = MoPubLog.getLogLevel();
        boolean debugModeEnabled = logLevel == MoPubLog.LogLevel.DEBUG;
        UnityAds.setDebugMode(debugModeEnabled);

        UnityAds.addListener(new HeaderBiddingListener() {
            @Override
            public void onUnityAdsTokenReady(String s) {
                UnityAdsAdapterConfiguration.this.requestingToken = false;
                UnityAdsAdapterConfiguration.this.token = s;
            }
        });
    }

    public static class HeaderBiddingListener implements IHeaderBiddingListener, IUnityAdsExtendedListener {

        @Override
        public void onUnityAdsReady(String s) {

        }

        @Override
        public void onUnityAdsStart(String s) {

        }

        @Override
        public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {

        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {

        }

        @Override
        public void onUnityAdsTokenReady(String s) {

        }

        @Override
        public void onUnityAdsBidLoaded(String uuid) {

        }

        @Override
        public void onUnityAdsBidFailedToLoad(String uuid) {

        }

        @Override
        public void onUnityAdsClick(String s) {

        }

        @Override
        public void onUnityAdsPlacementStateChanged(String s, UnityAds.PlacementState placementState, UnityAds.PlacementState placementState1) {

        }
    }
}
