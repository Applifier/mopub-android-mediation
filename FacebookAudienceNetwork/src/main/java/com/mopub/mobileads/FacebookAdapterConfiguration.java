package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BidderTokenProvider;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.facebookaudiencenetwork.BuildConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class FacebookAdapterConfiguration extends BaseAdapterConfiguration {
    private static final String NATIVE_BANNER_KEY = "native_banner";
    private static final String PLACEMENT_IDS_KEY = "placement_ids";

    private static final String ADAPTER_VERSION = BuildConfig.VERSION_NAME;
    private static final String MOPUB_NETWORK_NAME = BuildConfig.NETWORK_NAME;
    private static final String SDK_VERSION = com.facebook.ads.BuildConfig.VERSION_NAME;

    private static Boolean isNativeBanner;

    private final AtomicReference<String> tokenReference = new AtomicReference<>(null);
    private final AtomicBoolean isComputingToken = new AtomicBoolean(false);

    @NonNull
    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        refreshBidderToken(context);
        return tokenReference.get();
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return SDK_VERSION;
    }

    @Override
    public void initializeNetwork(@NonNull final Context context,
                                  @Nullable final Map<String, String> configuration,
                                  @NonNull final OnNetworkInitializationFinishedListener listener) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        synchronized (FacebookAdapterConfiguration.class) {
            try {
                tokenReference.set(BidderTokenProvider.getBidderToken(context));
                List<String> placementIds = new ArrayList<>();

                if (configuration != null && !configuration.isEmpty()) {
                    final String rawPlacementIds = configuration.get(PLACEMENT_IDS_KEY);

                    if (!TextUtils.isEmpty(rawPlacementIds)) {
                        placementIds = Arrays.asList(rawPlacementIds.split("\\s*,\\s*"));
                    }

                    isNativeBanner = Boolean.valueOf(configuration.get(NATIVE_BANNER_KEY));

                    setNativeBannerPref(isNativeBanner);
                }

                if (!AudienceNetworkAds.isInitialized(context)) {
                    AudienceNetworkAds.buildInitSettings(context)
                            .withPlacementIds(placementIds)
                            .withMediationService("MOPUB_" + MoPub.SDK_VERSION + ":" + ADAPTER_VERSION)
                            .initialize();
                }
            } catch (Throwable t) {
                MoPubLog.log(
                        CUSTOM_WITH_THROWABLE,
                        "Initializing Facebook Audience Network" + " has encountered an " +
                                "exception.",
                        t);
            }
        }
        listener.onNetworkInitializationFinished(this.getClass(),
                MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
    }

    public static Boolean getNativeBannerPref() {
        return isNativeBanner;
    }

    private static void setNativeBannerPref(Boolean pref) {
        isNativeBanner = pref;
    }

    private void refreshBidderToken(final Context context) {
        if (isComputingToken.compareAndSet(false, true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String token = BidderTokenProvider.getBidderToken(context);
                    if (token != null) {
                        tokenReference.set(token);
                    }
                    isComputingToken.set(false);
                }
            }).start();
        }
    }
}
