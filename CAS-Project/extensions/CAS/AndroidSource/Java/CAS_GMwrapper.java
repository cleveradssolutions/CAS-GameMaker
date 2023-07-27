package ${YYAndroidPackageName};

import ${YYAndroidPackageName}.R;
import com.yoyogames.runner.RunnerJNILib;

import androidx.annotation.NonNull;

import com.cleversolutions.ads.AdCallback;
import com.cleversolutions.ads.AdStatusHandler;
import com.cleversolutions.ads.AdType;
import com.cleversolutions.ads.ConsentFlow;
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

    //private final static int AD_TYPE_BANNER = 1 << 0;
    private final static int AD_TYPE_INTERSTITIAL = 1 << 1;
    private final static int AD_TYPE_REWARDED = 1 << 2;

    private static MediationManager mediationManager;
    private static AdCallback rewardedAdCallback, interstitialCallback;

    //public static void initialize(String casId, double adTypes, double taggedAudience, double consentStatus, double ccpaStatus, double testMode) {
    public static void initialize(String casId, double adTypes, String privacyPolicyURL, double testMode) {
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
                .withConsentFlow(new ConsentFlow().withPrivacyPolicy(privacyPolicyURL))
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
