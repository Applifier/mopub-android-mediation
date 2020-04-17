package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.headerbidding.IHeaderBiddingListener;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

import java.util.Map;
import java.util.UUID;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;

public class UnityInterstitial extends CustomEventInterstitial implements IUnityAdsExtendedListener {

    private static final String ADAPTER_NAME = UnityInterstitial.class.getSimpleName();

    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private Context mContext;
    private String mPlacementId = "video";
    private int impressionOrdinal;
    private int missedImpressionOrdinal;
    private String mUUID = UUID.randomUUID().toString();
    private boolean mBidLoaded = false;
    private boolean mUseHeaderBidding = false;
    @NonNull
    private UnityAdsAdapterConfiguration mUnityAdsAdapterConfiguration;

    public UnityInterstitial() {
        mUnityAdsAdapterConfiguration = new UnityAdsAdapterConfiguration();
    }

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {

        mPlacementId = UnityRouter.placementIdForServerExtras(serverExtras, mPlacementId);
        mCustomEventInterstitialListener = customEventInterstitialListener;
        mContext = context;

        final String adm = serverExtras.get(DataKeys.ADM_KEY);

        if (!TextUtils.isEmpty(adm)) {
            mUseHeaderBidding = true;
            UnityAds.addListener(new UnityAdsAdapterConfiguration.HeaderBiddingListener() {
                @Override
                public void onUnityAdsBidLoaded(String uuid) {
                    if (uuid == UnityInterstitial.this.mUUID) {
                        UnityAds.removeListener(this);
                        UnityInterstitial.this.onUnityAdsBidLoaded();
                    }
                }

                @Override
                public void onUnityAdsBidFailedToLoad(String uuid) {
                    if (uuid == UnityInterstitial.this.mUUID) {
                        UnityAds.removeListener(this);
                        UnityInterstitial.this.onUnityAdsBidFailedToLoad();
                    }
                }
            });

            UnityAds.loadBid(mUUID, mPlacementId, adm);
        }

        UnityAds.load(mPlacementId);

        mUnityAdsAdapterConfiguration.setCachedInitializationParameters(context, serverExtras);

        UnityRouter.getInterstitialRouter().addListener(mPlacementId, this);
        UnityRouter.getInterstitialRouter().setCurrentPlacementId(mPlacementId);
        initializeUnityAdsSdk(serverExtras);
    }

    private void initializeUnityAdsSdk(Map<String, String> serverExtras) {
        if (!UnityAds.isInitialized()) {
            if (!(mContext instanceof Activity)) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Context is null or is not an instanceof Activity.");
                return;
            }
            UnityRouter.initUnityAds(serverExtras, (Activity) mContext);
        }
    }

    public boolean hasVideoAvailable() {
        if (mUseHeaderBidding) {
            return  UnityAds.isReady(mPlacementId) && mBidLoaded;
        }
        return UnityAds.isReady(mPlacementId);
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (hasVideoAvailable() && mContext != null) {
            MediationMetaData metadata = new MediationMetaData(mContext);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

            if (mUseHeaderBidding) {
                UnityAds.show((Activity) mContext, mUUID);
            } else {
                UnityAds.show((Activity) mContext, mPlacementId);
            }
        } else {
            // lets Unity Ads know when ads fail to show
            MediationMetaData metadata = new MediationMetaData(mContext);
            metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
            metadata.commit();

            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Attempted to show Unity interstitial video before it was available.");
        }
    }

    @Override
    protected void onInvalidate() {
        UnityRouter.getInterstitialRouter().removeListener(mPlacementId);
        mCustomEventInterstitialListener = null;
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if (mUseHeaderBidding) {
            return;
        }
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialLoaded();

            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialShown();
        }

        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        if (mCustomEventInterstitialListener != null) {
            if (finishState == UnityAds.FinishState.ERROR) {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video encountered a playback error for " +
                        "placement " + placementId);
                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

                MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video completed for placement " + placementId);
                mCustomEventInterstitialListener.onInterstitialDismissed();
            }
        }
        UnityRouter.getInterstitialRouter().removeListener(placementId);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialClicked();
        }

        MoPubLog.log(CLICKED, ADAPTER_NAME);
    }


    // @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
        if (mUseHeaderBidding) {
            return;
        }
        if (placementId.equals(mPlacementId) && mCustomEventInterstitialListener != null) {
            if (newState == UnityAds.PlacementState.NO_FILL) {
                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                UnityRouter.getInterstitialRouter().removeListener(mPlacementId);

                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                        MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {

        if (mCustomEventInterstitialListener != null) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unity interstitial video cache failed for placement " +
                    mPlacementId + "." + message);
            MoPubErrorCode errorCode = UnityRouter.UnityAdsUtils.getMoPubErrorCode(unityAdsError);
            mCustomEventInterstitialListener.onInterstitialFailed(errorCode);

            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    errorCode.getIntCode(),
                    errorCode);
        }
    }

    public void onUnityAdsBidLoaded() {
        mBidLoaded = true;

        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialLoaded();

            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        }
    }

    public void onUnityAdsBidFailedToLoad() {
        mBidLoaded = false;

        mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
        UnityRouter.getInterstitialRouter().removeListener(mPlacementId);

        MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                MoPubErrorCode.NETWORK_NO_FILL);
    }
}
