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

import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.cleversolutions.ads.AdSize;
import com.cleversolutions.ads.AdError;
import com.cleversolutions.ads.AdViewListener;
import com.cleversolutions.ads.android.CASBannerView;

public class CAS_GMwrapper {
    private static final int EVENT_OTHER_SOCIAL = 70;
    private static final String TAG = "CAS_GM";

    private MediationManager cas; 
    private boolean casInitialized = false;
	
    private String casIdSaved = null;

    private CASBannerView bannerView = null;
    private FrameLayout bannerHost = null;

    private int bannerPosition = 0;

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
                        .withAdTypes(AdType.Interstitial, AdType.Rewarded, AdType.Banner)
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
						
						casIdSaved = casId;

            } catch (Throwable t) {
                Log.e(TAG, "CAS_Init failed", t);
                casInitialized = false;
                sendInitAsync(false, t.getMessage(), null, false, -1);
            }
        });
    }

    public void CAS_BannerCreate(final double sizeType, final double position,
                                 final double maxWidthDp, final double maxHeightDp,
                                 final double autoload) {
        final Activity act = activity();
        if (act == null) return;

        act.runOnUiThread(() -> {
            try {
                bannerPosition = (position == 1.0) ? 1 : 0;

                if (casIdSaved == null || casIdSaved.isEmpty()) {
                    sendBannerEvent("failed", "CAS ID is null. Call CAS_Init first.");
                    return;
                }

                ensureBannerHost(act);

                internalBannerDestroy();

                bannerView = new CASBannerView(act, casIdSaved);

                AdSize adSize;
                int st = (int) sizeType;
                switch (st) {
                    case 1:
                        adSize = AdSize.getSmartBanner(act);
                        break;
                    case 2:
                        adSize = AdSize.MEDIUM_RECTANGLE;
                        break;
                    case 3:
                        adSize = AdSize.LEADERBOARD;
                        break;
					case 4: {
						int w = (int) Math.max(0, maxWidthDp);
						adSize = AdSize.getAdaptiveBanner(act, w);
						break;
					}
					case 5: {
						int w = (int) Math.max(0, maxWidthDp);
						int h = (int) Math.max(0, maxHeightDp);
						adSize = AdSize.getAdaptiveBanner(act, w, h);
						break;
					}
                    case 0:
                    default:
                        adSize = AdSize.BANNER;
                        break;
                }

                bannerView.setSize(adSize);
                bannerView.setAutoloadEnabled(autoload == 1.0);

                bannerView.setAdListener(bannerListener);

                bannerHost.removeAllViews();

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.gravity = (bannerPosition == 1) ? (Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                                                   : (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

                bannerHost.addView(bannerView, lp);
				bannerView.setVisibility(View.VISIBLE);
				bannerView.requestLayout();
				bannerHost.requestLayout();
				bannerHost.bringToFront();

                sendBannerEvent("created", null);
            } catch (Throwable t) {
                Log.e(TAG, "CAS_BannerCreate failed", t);
                sendBannerEvent("failed", t.getMessage());
            }
        });
    }

    public void CAS_BannerLoad() {
        final Activity act = activity();
        if (act == null) return;
        act.runOnUiThread(() -> {
            if (bannerView != null) bannerView.load();
        });
    }

    public void CAS_BannerShow() {
        final Activity act = activity();
        if (act == null) return;
        act.runOnUiThread(() -> {
            if (bannerHost != null) bannerHost.setVisibility(View.VISIBLE);
        });
    }

    public void CAS_BannerHide() {
        final Activity act = activity();
        if (act == null) return;
        act.runOnUiThread(() -> {
            if (bannerHost != null) bannerHost.setVisibility(View.GONE);
        });
    }

    public void CAS_BannerDestroy() {
        final Activity act = activity();
        if (act == null) return;
        act.runOnUiThread(() -> {
            internalBannerDestroy();
            sendBannerEvent("destroyed", null);
        });
    }

    public double CAS_BannerIsCreated() {
        return (bannerView != null) ? 1.0 : 0.0;
    }

    public void CAS_BannerSetRefreshInterval(final double seconds) {
        final Activity act = activity();
        if (act == null) return;
        act.runOnUiThread(() -> {
            if (bannerView != null) bannerView.setRefreshInterval((int)Math.max(0, seconds));
        });
    }

    public void CAS_BannerDisableRefresh() {
        final Activity act = activity();
        if (act == null) return;
        act.runOnUiThread(() -> {
            if (bannerView != null) bannerView.disableAdRefresh();
        });
    }

    private void ensureBannerHost(Activity act) {
        if (bannerHost != null) return;

        ViewGroup content = act.findViewById(android.R.id.content);
        bannerHost = new FrameLayout(act);
        bannerHost.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        bannerHost.setClickable(false);
        bannerHost.setFocusable(false);

        content.addView(bannerHost);
    }

    private void internalBannerDestroy() {
        try {
            if (bannerHost != null) bannerHost.removeAllViews();
            if (bannerView != null) {
                bannerView.destroy();
                bannerView = null;
            }
        } catch (Throwable ignored) {}
    }

    private final AdViewListener bannerListener = new AdViewListener() {
        @Override
        public void onAdViewLoaded(@NonNull CASBannerView view) {
            Log.d(TAG, "Banner loaded");
            sendBannerEvent("loaded", null);
        }

        @Override
        public void onAdViewFailed(@NonNull CASBannerView view, @NonNull AdError error) {
            String msg = error.getMessage();
            Log.d(TAG, "Banner failed: " + msg);
            sendBannerEvent("failed", msg);
        }

        @Override
        public void onAdViewClicked(@NonNull CASBannerView view) {
            Log.d(TAG, "Banner clicked");
            sendBannerEvent("clicked", null);
        }

        @Override
        public void onAdViewPresented(@NonNull CASBannerView view, @NonNull AdStatusHandler info) {
            Log.d(TAG, "Banner presented");
            sendBannerEvent("presented", null);
        }
    };

    private void sendBannerEvent(String state, String messageOrNull) {
        int map = RunnerJNILib.jCreateDsMap(null, null, null);
        RunnerJNILib.DsMapAddString(map, "type", "cas_banner");
        RunnerJNILib.DsMapAddString(map, "state", state);
        if (messageOrNull != null) RunnerJNILib.DsMapAddString(map, "error", messageOrNull);
        RunnerJNILib.CreateAsynEventWithDSMap(map, EVENT_OTHER_SOCIAL);
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
        @Override public void onClicked() { /* можно послать событие */ }
        @Override public void onComplete() { /* у межстранички нет награды */ }
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
