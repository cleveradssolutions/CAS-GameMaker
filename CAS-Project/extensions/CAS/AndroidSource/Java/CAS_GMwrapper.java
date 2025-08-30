package ${YYAndroidPackageName};

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.yoyogames.runner.RunnerJNILib;
import ${YYAndroidPackageName}.RunnerActivity;

import com.cleversolutions.ads.android.CAS;
import com.cleversolutions.ads.MediationManager;
import com.cleversolutions.ads.InitializationListener;
import com.cleversolutions.ads.InitialConfiguration;
import com.cleversolutions.ads.ConsentFlow;
import com.cleversolutions.ads.AdType;
import com.cleversolutions.ads.AdCallback;
import com.cleversolutions.ads.AdStatusHandler;

public class CAS_GMwrapper {
    private static final int EVENT_OTHER_SOCIAL = 70;
    private static final String TAG = "CAS_GM";

    private MediationManager cas; 
    private boolean casInitialized = false;

    private static Activity activity() {
        return RunnerActivity.CurrentActivity;
    }

    public void CAS_Init(final String casId, final double testMode) {
        final Activity act = activity();
        if (act == null) {
            sendInitAsync(false, "Activity is null", null, false, -1);
            return;
        }

        act.runOnUiThread(() -> {
            try {
                Log.d(TAG, "Initializing CAS. casId=" + casId + " testMode=" + (testMode == 1));

                cas = CAS.buildManager()
                        .withCasId(casId)
                        .withTestAdMode(testMode == 1)
                        .withAdTypes(AdType.Interstitial, AdType.Rewarded)
                        .withConsentFlow(new ConsentFlow(true))
                        .withCompletionListener(new InitializationListener() {
                            @Override
                            public void onCASInitialized(@NonNull InitialConfiguration config) {
                                String err = config.getError();
                                boolean ok = (err == null);
                                casInitialized = ok;

                                String country = config.getCountryCode();
                                boolean consentRequired = config.isConsentRequired();
                                int consentStatus = config.getConsentFlowStatus();

                                Log.d(TAG, "CAS init complete. ok=" + ok
                                        + " country=" + country
                                        + " consentRequired=" + consentRequired
                                        + " consentStatus=" + consentStatus
                                        + (ok ? "" : (" error=" + err)));

                                sendInitAsync(ok, err, country, consentRequired, consentStatus);
                            }
                        })
                        .build(act);

            } catch (Throwable t) {
                Log.e(TAG, "CAS_Init failed", t);
                casInitialized = false;
                sendInitAsync(false, t.getMessage(), null, false, -1);
            }
        });
    }

    public void CAS_LoadInterstitial() {
        if (cas != null) cas.loadInterstitial();
    }

    public double CAS_IsInterstitialReady() {
        return (cas != null && cas.isInterstitialReady()) ? 1.0 : 0.0;
    }

    public void CAS_ShowInterstitial() {
        final Activity act = activity();
        if (cas != null && act != null && cas.isInterstitialReady()) {
            act.runOnUiThread(() -> cas.showInterstitial(act, interstitialCb));
        }
    }

    public void CAS_LoadRewarded() {
        if (cas != null) cas.loadRewardedAd();
    }

    public double CAS_IsRewardedReady() {
        return (cas != null && cas.isRewardedAdReady()) ? 1.0 : 0.0;
    }

    public void CAS_ShowRewarded() {
        final Activity act = activity();
        if (cas != null && act != null && cas.isRewardedAdReady()) {
            act.runOnUiThread(() -> cas.showRewardedAd(act, rewardedCb));
        }
    }
	
    private final AdCallback interstitialCb = new AdCallback() {
        @Override public void onShown(@NonNull AdStatusHandler ad) { 
            Log.d(TAG, "Interstitial shown");
            sendInterstitialEvent("shown", null);
        }
        @Override public void onShowFailed(@NonNull String message) { 
            Log.d(TAG, "Interstitial show failed: " + message);
            sendInterstitialEvent("failed", message);
        }
        @Override public void onClicked() { /* при желании можно послать событие */ }
        @Override public void onComplete() { /* у interstitial нет награды */ }
        @Override public void onClosed() { 
            Log.d(TAG, "Interstitial closed");
            sendInterstitialEvent("closed", null);
        }
    };

    private final AdCallback rewardedCb = new AdCallback() {
        @Override public void onShown(@NonNull AdStatusHandler ad) { 
            Log.d(TAG, "Rewarded shown");
            sendRewardedEvent("shown", null);
        }
        @Override public void onShowFailed(@NonNull String message) { 
            Log.d(TAG, "Rewarded show failed: " + message);
            sendRewardedEvent("failed", message);
        }
        @Override public void onClicked() { /* при желании можно послать событие */ }
        @Override public void onComplete() { 
            Log.d(TAG, "Reward granted");
            sendRewardedEvent("complete", null);
        }
        @Override public void onClosed() { 
            Log.d(TAG, "Rewarded closed");
            sendRewardedEvent("closed", null);
        }
    };

    public double CAS_IsInitialized() {
        return casInitialized ? 1.0 : 0.0;
    }

    private void sendInitAsync(boolean ok, String errorOrNull, String countryOrNull,
                               boolean consentRequired, int consentStatus) {
        int map = RunnerJNILib.jCreateDsMap(null, null, null);
        RunnerJNILib.DsMapAddString(map, "type", "cas_init");
        RunnerJNILib.DsMapAddDouble(map, "success", ok ? 1 : 0);
        if (errorOrNull != null) RunnerJNILib.DsMapAddString(map, "error", errorOrNull);
        if (countryOrNull != null) RunnerJNILib.DsMapAddString(map, "country", countryOrNull);
        RunnerJNILib.DsMapAddDouble(map, "consent_required", consentRequired ? 1 : 0);
        RunnerJNILib.DsMapAddDouble(map, "consent_status", consentStatus);
        RunnerJNILib.CreateAsynEventWithDSMap(map, EVENT_OTHER_SOCIAL);
    }

    private void sendInterstitialEvent(String state, String messageOrNull) {
        int map = RunnerJNILib.jCreateDsMap(null, null, null);
        RunnerJNILib.DsMapAddString(map, "type", "cas_interstitial");
        RunnerJNILib.DsMapAddString(map, "state", state);  
        if (messageOrNull != null) RunnerJNILib.DsMapAddString(map, "error", messageOrNull);
        RunnerJNILib.CreateAsynEventWithDSMap(map, EVENT_OTHER_SOCIAL);
    }

    private void sendRewardedEvent(String state, String messageOrNull) {
        int map = RunnerJNILib.jCreateDsMap(null, null, null);
        RunnerJNILib.DsMapAddString(map, "type", "cas_rewarded");
        RunnerJNILib.DsMapAddString(map, "state", state);  
        if (messageOrNull != null) RunnerJNILib.DsMapAddString(map, "error", messageOrNull);
        RunnerJNILib.CreateAsynEventWithDSMap(map, EVENT_OTHER_SOCIAL);
    }
}
