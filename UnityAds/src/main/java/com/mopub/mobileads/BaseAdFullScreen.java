package com.mopub.mobileads;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mopub.common.DataKeys;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;

import java.util.Map;
import java.util.UUID;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.unity3d.ads.UnityAds.UnityAdsError.SHOW_ERROR;

public abstract class BaseAdFullScreen extends BaseAd implements IUnityAdsExtendedListener {

    @NonNull
    protected String mPlacementId = "";
    private String mObjectId;

    private Context mContext;
    private int impressionOrdinal;
    private int missedImpressionOrdinal;

    /**
     * IUnityAdsLoadListener instance. Contains ad load success and fail logic.
     */
    private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String placementId) {
            MoPubLog.log(CUSTOM, getAdapterName(), String.format("Unity %s successfully loaded for placementId %i", getAdTypeName(), placementId));
            MoPubLog.log(LOAD_SUCCESS, getAdapterName());

            if (mLoadListener != null) {
                mLoadListener.onAdLoaded();
            }
        }

        @Override
        public void onUnityAdsFailedToLoad(String placementId) {
            MoPubLog.log(CUSTOM, getAdapterName(), String.format("Unity %s failed to load for placement %i", getAdTypeName(), placementId));
            MoPubLog.log(LOAD_FAILED, getAdapterName(), MoPubErrorCode.NETWORK_NO_FILL.getIntCode(), MoPubErrorCode.NETWORK_NO_FILL);

            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    };

    @Override
    protected void onInvalidate() {
        UnityAds.removeListener(BaseAdFullScreen.this);
    }

    @Override
    protected void load(@NonNull Context context, @NonNull AdData adData) throws Exception {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        final Map<String, String> extras = adData.getExtras();
        mPlacementId = UnityRouter.placementIdForServerExtras(extras, mPlacementId);

        setAutomaticImpressionAndClickTracking(false);

        //Calls abstract function that must be implemented
        adLoad(context, adData);

        String markup = extras.get(DataKeys.ADM_KEY);

        if (markup != null) {
            mObjectId = UUID.randomUUID().toString();
            UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();
            loadOptions.setAdMarkup(markup);
            loadOptions.setObjectId(mObjectId);
            UnityAds.load(mPlacementId, loadOptions, mUnityLoadListener);
        } else {
            UnityAds.load(mPlacementId, mUnityLoadListener);
        }
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        MoPubLog.log(CUSTOM, getAdapterName(), String.format("Unity %s clicked for placement %i.", getAdTypeName(), placementId));
        MoPubLog.log(CLICKED, getAdapterName());

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) { }

    @Override
    public void onUnityAdsReady(String placementId) { }

    @Override
    public void onUnityAdsStart(String placementId) {
        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        }
        MoPubLog.log(CUSTOM, getAdapterName(), String.format("Unity %s started for placement %i.", getAdTypeName(), placementId));

        MoPubLog.log(SHOW_SUCCESS, getAdapterName());
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        MoPubLog.log(CUSTOM, getAdapterName(), "Unity Ad finished with finish state = " + finishState);

        if (finishState == UnityAds.FinishState.ERROR) {
            MoPubLog.log(CUSTOM, getAdapterName(),
                    String.format("Unity %s encountered a playback error for placement %i.",
                            getAdTypeName(), placementId));
            MoPubLog.log(SHOW_FAILED, getAdapterName(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            }

        } else if (finishState == UnityAds.FinishState.COMPLETED) {
            MoPubLog.log(SHOULD_REWARD, getAdapterName(), MoPubReward.NO_REWARD_AMOUNT, MoPubReward.NO_REWARD_LABEL);

            if (mInteractionListener != null) {
                mInteractionListener.onAdComplete(MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                        MoPubReward.DEFAULT_REWARD_AMOUNT));
                MoPubLog.log(CUSTOM, getAdapterName(),
                        String.format("Unity %s completed for placement %i.",
                        getAdTypeName(), placementId));
            }

        } else if (finishState == UnityAds.FinishState.SKIPPED) {
            MoPubLog.log(CUSTOM, getAdapterName(),
                    String.format("Unity %s was skipped, no reward will be given.",
                            getAdTypeName()));
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
        UnityAds.removeListener(BaseAdFullScreen.this);
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
        if (unityAdsError == SHOW_ERROR) {
            if (mContext != null) {
                // Lets Unity Ads know when ads fail to show
                MediationMetaData metadata = new MediationMetaData(mContext);
                metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
                metadata.commit();
            }

            MoPubLog.log(CUSTOM, getAdapterName(),
                    String.format("Failed to show Unity %s with error message: %s",
                            getAdTypeName(), message));
            MoPubLog.log(SHOW_FAILED, getAdapterName(),
                    MoPubErrorCode.NETWORK_NO_FILL.getIntCode(),
                    MoPubErrorCode.NETWORK_NO_FILL);

            if (mInteractionListener != null) {
                mInteractionListener.onAdFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }

            UnityAds.removeListener(BaseAdFullScreen.this);

        } else if (mLoadListener != null) {
            MoPubLog.log(CUSTOM, getAdapterName(),
                    String.format("Unity %s failed with error message: %s",
                            getAdTypeName(), message));
            MoPubLog.log(LOAD_FAILED, getAdapterName(),
                    MoPubErrorCode.UNSPECIFIED.getIntCode(),
                    MoPubErrorCode.UNSPECIFIED);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.UNSPECIFIED);
        }
    }

    abstract void adLoad(@NonNull Context context, @NonNull AdData adData);

    /**
     * Returns class name as string used for logging
     **/
    abstract String getAdapterName();
    /**
     * Returns ad type as string used for logging
     **/
    abstract String getAdTypeName();
}
