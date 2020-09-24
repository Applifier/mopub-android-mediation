package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;

public class UnityBanner extends BaseAd implements BannerView.IListener {

    private static final String ADAPTER_NAME = UnityBanner.class.getSimpleName();

    private String placementId = "banner";
    private BannerView mBannerView;

    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    public UnityBanner() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (!(context instanceof Activity)) {
            MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
            return;
        }

        setAutomaticImpressionAndClickTracking(false);

        final Map<String, String> extras = adData.getExtras();
        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, extras);

        placementId = UnityRouter.placementIdForServerExtras(extras, placementId);

        final String format = extras.get("adunit_format");
        final boolean isMediumRectangleFormat = format.contains("medium_rectangle");

        if (isMediumRectangleFormat) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Unity Ads does not support medium rectangle ads.");

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }

            return;
        }

        final BannerView.IListener bannerlistener = this;

        UnityRouter.initUnityAds(serverExtras, context, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                if (localExtras == null || localExtras.isEmpty()) {
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to get banner size because the " +
                            "localExtras is empty.");

                    if (customEventBannerListener != null) {
                        customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    }
                } else {
                    final UnityBannerSize bannerSize = unityAdsAdSizeFromLocalExtras(context, localExtras);

                    if (mBannerView != null) {
                        mBannerView.destroy();
                        mBannerView = null;
                    }

                    mBannerView = new BannerView((Activity) context, placementId, bannerSize);
                    mBannerView.setListener(bannerlistener);
                    mBannerView.load();

                    MoPubLog.log(getAdNetworkId(), LOAD_ATTEMPTED, ADAPTER_NAME);
                }
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String s) {
                MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to initialize Unity Ads");
                MoPubLog.log(getAdNetworkId(), LOAD_FAILED, ADAPTER_NAME, MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);

                if (customEventBannerListener != null) {
                    customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            }
        });
    }

    @Override
    @Nullable
    public View getAdView() {
        return mBannerView;
    }

    private UnityBannerSize unityAdsAdSizeFromAdData(@NonNull final AdData adData) {

        int adWidth = adData.getAdWidth() != null ? adData.getAdWidth() : 0;
        int adHeight = adData.getAdHeight() != null ? adData.getAdHeight() : 0;

        if (adWidth >= 728 && adHeight >= 90) {
            return new UnityBannerSize(728, 90);
        } else if (adWidth >= 468 && adHeight >= 60) {
            return new UnityBannerSize(468, 60);
        } else {
            return new UnityBannerSize(320, 50);
        }

    }

    @Override
    protected void onInvalidate() {
        if (mBannerView != null) {
            mBannerView.destroy();
        }

        mBannerView = null;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    public void onBannerLoaded(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);
        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);
        MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
            mBannerView = bannerView;
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdImpression();
        }
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo errorInfo) {
        MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, String.format("Banner did error for placement %s with error %s",
                placementId, errorInfo.errorMessage));

        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
    }

    @NonNull
    @Override
    public String getAdNetworkId() {
        return placementId != null ? placementId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        return false;
    }
}
