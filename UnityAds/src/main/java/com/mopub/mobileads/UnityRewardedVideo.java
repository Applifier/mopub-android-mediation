package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

public class UnityRewardedVideo extends BaseAdFullScreen {

    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final class UnityLifecycleListener extends BaseLifecycleListener {
        @Override
        public void onCreate(@NonNull final Activity activity) {
            super.onCreate(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
        }
    }

    @Override
    void adLoad(@NonNull Context context, @NonNull AdData adData) { }

    @Override
    String getAdapterName() {
        return UnityRewardedVideo.class.getSimpleName();
    }

    @Override
    String getAdTypeName() {
        return "rewarded";
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @NonNull

    @Override
    protected String getAdNetworkId() {
        return mPlacementId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull AdData adData) throws Exception {
        Preconditions.checkNotNull(launcherActivity);
        Preconditions.checkNotNull(adData);

        mContext = launcherActivity;

        final Map<String, String> extras = adData.getExtras();
        mPlacementId = UnityRouter.placementIdForServerExtras(extras, mPlacementId);

        if (UnityAds.isInitialized()) {
            return true;
        }

        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(launcherActivity, extras);

        if (UnityAds.isInitialized()) {
            return true;
        } else {
            UnityRouter.initUnityAds(extras, launcherActivity, new IUnityAdsInitializationListener() {
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
                        "Unity Ads adapter failed to request rewarded video ad, Unity Ads is not initialized yet. " +
                                "Failing this ad request and calling Unity Ads initialization, " +
                                "so it would be available for an upcoming ad request");
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
            return false;
        }
    }
}
