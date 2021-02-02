package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

public class UnityInterstitial extends BaseAdFullScreen {

    @Override
    void adLoad(@NonNull Context context, @NonNull AdData adData) {
        final Map<String, String> extras = adData.getExtras();
        if (!UnityAds.isInitialized()) {
            UnityRouter.initUnityAds(extras, context, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    MoPubLog.log(CUSTOM, getAdapterName(), "Unity Ads successfully initialized.");
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String errorMessage) {
                    if (errorMessage != null) {
                        MoPubLog.log(CUSTOM, getAdapterName(), "Unity Ads failed to initialize initialize with message: " + errorMessage);
                    }
                }
            });

            if (mLoadListener != null) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, getAdapterName(),
                        "Unity Ads adapter failed to request interstitial ad, Unity Ads is not initialized yet. " +
                                "Failing this ad request and calling Unity Ads initialization, " +
                                "so it would be available for an upcoming ad request");
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return;
        }
        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, extras);
    }

    @Override
    String getAdapterName() {
        return UnityInterstitial.class.getSimpleName();
    }

    @Override
    String getAdTypeName() {
        return "interstitial";
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mPlacementId != null ? mPlacementId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        return false;
    }
}
