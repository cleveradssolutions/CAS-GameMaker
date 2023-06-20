package ${YYAndroidPackageName};

import ${YYAndroidPackageName}.R;
import com.yoyogames.runner.RunnerJNILib;

import androidx.annotation.NonNull;

import com.cleversolutions.ads.AdCallback;
import com.cleversolutions.ads.AdStatusHandler;
import com.cleversolutions.ads.AdType;
import com.cleversolutions.ads.AdsSettings;
import com.cleversolutions.ads.Audience;
import com.cleversolutions.ads.CCPAStatus;
import com.cleversolutions.ads.ConsentStatus;
import com.cleversolutions.ads.InitialConfiguration;
import com.cleversolutions.ads.MediationManager;
import com.cleversolutions.ads.android.CAS;

import java.util.ArrayList;

public class CAS_GMwrapper {

    private final static int EVENT_OTHER_SOCIAL = 70;
    private final static int ASYNC_RESPONSE_INITIALIZED = 324400;
    private final static int ASYNC_RESPONSE_NOT_INITIALIZED = 324401;
    private final static int ASYNC_RESPONSE_ON_REWARD = 324402;
    private final static int ASYNC_RESPONSE_ON_INTERSTITIAL_COMPLETE = 324403;

    private final static int AUDIENCE_UNDEFINED = 0;
    private final static int AUDIENCE_CHILDREN = 1;
    private final static int AUDIENCE_NOT_CHILDREN = 2;

    private final static int CONSENT_STATUS_UNDEFINED = 0;
    private final static int CONSENT_STATUS_ACCEPTED = 1;
    private final static int CONSENT_STATUS_DENIED = 2;

    private final static int CCPA_STATUS_UNDEFINED = 0;
    private final static int CCPA_STATUS_OPT_OUT_SALE = 1;
    private final static int CCPA_STATUS_OPT_IN_SALE = 2;

    //private final static int AD_TYPE_BANNER = 1 << 0;
    private final static int AD_TYPE_INTERSTITIAL = 1 << 1;
    private final static int AD_TYPE_REWARDED = 1 << 2;

    private static MediationManager mediationManager;
    private static AdCallback rewardedAdCallback, interstitialCallback;

    public static void initialize(String casId, double adTypes, double taggedAudience, double consentStatus, double ccpaStatus, double testMode) {
        int adTypesInt = (int)Math.round(adTypes);
        ArrayList<AdType> adTypesAL = new ArrayList<>(1);
        if ((adTypesInt & AD_TYPE_INTERSTITIAL) == AD_TYPE_INTERSTITIAL)
            adTypesAL.add(AdType.Interstitial);
        if ((adTypesInt & AD_TYPE_REWARDED) == AD_TYPE_REWARDED)
            adTypesAL.add(AdType.Rewarded);

        AdType[] adTypesAr = new AdType[adTypesAL.size()];

        rewardedAdCallback = new AdCallback() {
            @Override
            public void onShown(@NonNull AdStatusHandler adStatusHandler) {}

            @Override
            public void onShowFailed(@NonNull String s) {}

            @Override
            public void onClicked() {}

            @Override
            public void onComplete() {
                int asyncCallback = RunnerJNILib.jCreateDsMap(null, null, null);
                RunnerJNILib.DsMapAddDouble(asyncCallback, "id", ASYNC_RESPONSE_ON_REWARD);
                RunnerJNILib.CreateAsynEventWithDSMap(asyncCallback, EVENT_OTHER_SOCIAL);
            }

            @Override
            public void onClosed() {}
        };

        interstitialCallback = new AdCallback() {
            @Override
            public void onShown(@NonNull AdStatusHandler adStatusHandler) {}

            @Override
            public void onShowFailed(@NonNull String s) {}

            @Override
            public void onClicked() {}

            @Override
            public void onComplete() {
                int asyncCallback = RunnerJNILib.jCreateDsMap(null, null, null);
                RunnerJNILib.DsMapAddDouble(asyncCallback, "id", ASYNC_RESPONSE_ON_INTERSTITIAL_COMPLETE);
                RunnerJNILib.CreateAsynEventWithDSMap(asyncCallback, EVENT_OTHER_SOCIAL);
            }

            @Override
            public void onClosed() {}
        };

        //tagged audience
        AdsSettings settings = CAS.getSettings();
        switch ((int)Math.round(taggedAudience)) {
            case AUDIENCE_UNDEFINED:
                settings.setTaggedAudience(Audience.UNDEFINED);
                break;
            case AUDIENCE_CHILDREN:
                settings.setTaggedAudience(Audience.CHILDREN);
                break;
            case AUDIENCE_NOT_CHILDREN:
                settings.setTaggedAudience(Audience.NOT_CHILDREN);
                break;
        }

        //consent status
        switch ((int)Math.round(consentStatus)) {
            case CONSENT_STATUS_UNDEFINED:
                settings.setUserConsent(ConsentStatus.UNDEFINED);
                break;
            case CONSENT_STATUS_ACCEPTED:
                settings.setUserConsent(ConsentStatus.ACCEPTED);
                break;
            case CONSENT_STATUS_DENIED:
                settings.setUserConsent(ConsentStatus.DENIED);
                break;
        }

        //CCPA status
        switch ((int)Math.round(ccpaStatus)) {
            case CCPA_STATUS_UNDEFINED:
                settings.setCcpaStatus(CCPAStatus.UNDEFINED);
                break;
            case CCPA_STATUS_OPT_OUT_SALE:
                settings.setCcpaStatus(CCPAStatus.OPT_OUT_SALE);
                break;
            case CCPA_STATUS_OPT_IN_SALE:
                settings.setCcpaStatus(CCPAStatus.OPT_IN_SALE);
                break;
        }

        mediationManager = CAS.buildManager()
                .withCasId(casId)
                .withCompletionListener((InitialConfiguration config) -> {
                    int initCallback = RunnerJNILib.jCreateDsMap(null, null, null);
                    if (config.getError() == null)
                        RunnerJNILib.DsMapAddDouble(initCallback, "id", ASYNC_RESPONSE_INITIALIZED);
                    else {
                        RunnerJNILib.DsMapAddDouble(initCallback, "id", ASYNC_RESPONSE_NOT_INITIALIZED);
                        RunnerJNILib.DsMapAddString(initCallback, "error", config.getError());
                    }
                    RunnerJNILib.CreateAsynEventWithDSMap(initCallback, EVENT_OTHER_SOCIAL);
                })
                .withAdTypes(adTypesAL.toArray(adTypesAr))
                .withTestAdMode(testMode == 1)
                .initialize(RunnerActivity.CurrentActivity);
    }

    public static double isRewardedAdReady() {
        return mediationManager == null ? 0 : (mediationManager.isRewardedAdReady() ? 1 : 0);
    }

    public static double showRewardedAd() {
        if (mediationManager == null)
            return 0;

        if (mediationManager.isRewardedAdReady()) {
            mediationManager.showRewardedAd(RunnerActivity.CurrentActivity, rewardedAdCallback);
            return 1;
        }
        else
            return 0;
    }

    public static double isInterstitialReady() {
        return mediationManager == null ? 0 : (mediationManager.isInterstitialReady() ? 1 : 0);
    }

    public static double showInterstitialAd() {
        if (mediationManager == null)
            return 0;

        if (mediationManager.isInterstitialReady()) {
            mediationManager.showInterstitial(RunnerActivity.CurrentActivity, interstitialCallback);
            return 1;
        }
        else
            return 0;
    }

}
