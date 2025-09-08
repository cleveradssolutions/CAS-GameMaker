package ${YYAndroidPackageName};

import ${YYAndroidPackageName}.R;
import ${YYAndroidPackageName}.RunnerActivity;

import com.yoyogames.runner.RunnerJNILib;

import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.cleversolutions.ads.AdCallback;
import com.cleversolutions.ads.AdError;
import com.cleversolutions.ads.AdSize;
import com.cleversolutions.ads.AdStatusHandler;
import com.cleversolutions.ads.AdType;
import com.cleversolutions.ads.ConsentFlow;
import com.cleversolutions.ads.InitialConfiguration;
import com.cleversolutions.ads.MediationManager;
import com.cleversolutions.ads.android.CAS;
import com.cleversolutions.ads.android.CASBannerView;
import com.cleversolutions.ads.AdViewListener;
import com.cleversolutions.ads.AdError;

import java.util.ArrayList;

public class CAS_GMwrapper {

    private static final int EVENT_OTHER_SOCIAL = 70;
    private static final int ASYNC_RESPONSE_INITIALIZED = 324400;
    private static final int ASYNC_RESPONSE_NOT_INITIALIZED = 324401;
    private static final int ASYNC_RESPONSE_ON_REWARD = 324402;
    private static final int ASYNC_RESPONSE_ON_INTERSTITIAL_COMPLETE = 324403;

    private static final int AD_TYPE_BANNER = 1;
    private static final int AD_TYPE_INTERSTITIAL = 1 << 1;
    private static final int AD_TYPE_REWARDED = 1 << 2;

    private static MediationManager mediationManager;
    private static AdCallback rewardedAdCallback, interstitialCallback;

    private static CASBannerView bannerViewTop;
    private static CASBannerView bannerViewBottom;
    private static CASBannerView bannerViewBig;

    public static void initialize(String casId, double adTypes, String privacyPolicyURL, double testMode) {
        int adTypesInt = (int) Math.round(adTypes);
        ArrayList<AdType> adTypesAL = new ArrayList<>(3);
        if ((adTypesInt & AD_TYPE_BANNER) == AD_TYPE_BANNER) adTypesAL.add(AdType.Banner);
        if ((adTypesInt & AD_TYPE_INTERSTITIAL) == AD_TYPE_INTERSTITIAL) adTypesAL.add(AdType.Interstitial);
        if ((adTypesInt & AD_TYPE_REWARDED) == AD_TYPE_REWARDED) adTypesAL.add(AdType.Rewarded);

        rewardedAdCallback = new AdCallback() {
            @Override public void onShown(@NonNull AdStatusHandler adStatusHandler) {}
            @Override public void onShowFailed(@NonNull String s) {}
            @Override public void onClicked() {}
            @Override public void onComplete() {
                int asyncCallback = RunnerJNILib.jCreateDsMap(null, null, null);
                RunnerJNILib.DsMapAddDouble(asyncCallback, "id", ASYNC_RESPONSE_ON_REWARD);
                RunnerJNILib.CreateAsynEventWithDSMap(asyncCallback, EVENT_OTHER_SOCIAL);
            }
            @Override public void onClosed() {}
        };

        interstitialCallback = new AdCallback() {
            @Override public void onShown(@NonNull AdStatusHandler adStatusHandler) {}
            @Override public void onShowFailed(@NonNull String s) {}
            @Override public void onClicked() {}
            @Override public void onComplete() {
                int asyncCallback = RunnerJNILib.jCreateDsMap(null, null, null);
                RunnerJNILib.DsMapAddDouble(asyncCallback, "id", ASYNC_RESPONSE_ON_INTERSTITIAL_COMPLETE);
                RunnerJNILib.CreateAsynEventWithDSMap(asyncCallback, EVENT_OTHER_SOCIAL);
            }
            @Override public void onClosed() {}
        };

        AdType[] adTypesArr = adTypesAL.toArray(new AdType[0]);

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            mediationManager = CAS.buildManager()
                    .withCasId(casId)
                    .withAdTypes(adTypesArr)
                    .withTestAdMode(testMode == 1)
                    .withConsentFlow(new ConsentFlow().withPrivacyPolicy(privacyPolicyURL))
                    .withCompletionListener((InitialConfiguration config) -> {
                        int initCallback = RunnerJNILib.jCreateDsMap(null, null, null);
                        if (config.getError() == null) {
                            RunnerJNILib.DsMapAddDouble(initCallback, "id", ASYNC_RESPONSE_INITIALIZED);
                        } else {
                            RunnerJNILib.DsMapAddDouble(initCallback, "id", ASYNC_RESPONSE_NOT_INITIALIZED);
                            RunnerJNILib.DsMapAddString(initCallback, "error", config.getError());
                        }
                        RunnerJNILib.CreateAsynEventWithDSMap(initCallback, EVENT_OTHER_SOCIAL);
                    })
                    .initialize(RunnerActivity.CurrentActivity);
        });
    }


    public static double isRewardedAdReady() {
        return mediationManager != null && mediationManager.isRewardedAdReady() ? 1 : 0;
    }

    public static double showRewardedAd() {
        if (mediationManager == null) return 0;
        if (!mediationManager.isRewardedAdReady()) return 0;
        RunnerActivity.CurrentActivity.runOnUiThread(() ->
                mediationManager.showRewardedAd(RunnerActivity.CurrentActivity, rewardedAdCallback));
        return 1;
    }

    public static double isInterstitialReady() {
        return mediationManager != null && mediationManager.isInterstitialReady() ? 1 : 0;
    }

    public static double showInterstitialAd() {
        if (mediationManager == null) return 0;
        if (!mediationManager.isInterstitialReady()) return 0;
        RunnerActivity.CurrentActivity.runOnUiThread(() ->
                mediationManager.showInterstitial(RunnerActivity.CurrentActivity, interstitialCallback));
        return 1;
    }


    private static AdViewListener makeBannerListener(final String where) {
        return new AdViewListener() {
            @Override public void onAdViewLoaded(@NonNull CASBannerView view) {
                sendBannerEvent("loaded", where, null);
            }
            @Override public void onAdViewFailed(@NonNull CASBannerView view, @NonNull AdError error) {
                sendBannerEvent("failed", where, error.getMessage());
            }
            @Override public void onAdViewClicked(@NonNull CASBannerView view) {
                sendBannerEvent("clicked", where, null);
            }
            @Override public void onAdViewPresented(@NonNull CASBannerView view, @NonNull AdStatusHandler info) {
                sendBannerEvent("impression", where, null);
            }
        };
    }

    public static void showBannerAd_Bottom() {
        if (mediationManager == null || bannerViewBottom != null) return;

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            View rootView = RunnerActivity.CurrentActivity.findViewById(android.R.id.content).getRootView();
            bannerViewBottom = new CASBannerView(RunnerActivity.CurrentActivity, mediationManager);
            bannerViewBottom.setSize(AdSize.getAdaptiveBannerInScreen(RunnerActivity.CurrentActivity));
            bannerViewBottom.setAutoloadEnabled(true);
            bannerViewBottom.setAdListener(makeBannerListener("bottom"));

            if (rootView instanceof FrameLayout) {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM
                );
                bannerViewBottom.setLayoutParams(lp);
            } else {
                bannerViewBottom.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bannerViewBottom.setForegroundGravity(Gravity.CENTER);
            }
            ((ViewGroup) rootView).addView(bannerViewBottom);
            sendBannerEvent("shown", "bottom", null);
        });
    }

    public static void removeBannerAd_Bottom() {
        if (mediationManager == null) return;

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            ViewGroup rootView = (ViewGroup) RunnerActivity.CurrentActivity
                    .findViewById(android.R.id.content).getRootView();
            if (bannerViewBottom != null) {
                rootView.removeView(bannerViewBottom);
                bannerViewBottom.destroy();
                bannerViewBottom = null;
                sendBannerEvent("destroyed", "bottom", null);
            }
        });
    }

    public static void showBannerAd_Top() {
        if (mediationManager == null || bannerViewTop != null) return;

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            View rootView = RunnerActivity.CurrentActivity.findViewById(android.R.id.content).getRootView();
            bannerViewTop = new CASBannerView(RunnerActivity.CurrentActivity, mediationManager);
            bannerViewTop.setSize(AdSize.getAdaptiveBannerInScreen(RunnerActivity.CurrentActivity));
            bannerViewTop.setAutoloadEnabled(true);
            bannerViewTop.setAdListener(makeBannerListener("top"));

            if (rootView instanceof FrameLayout) {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP
                );
                bannerViewTop.setLayoutParams(lp);
            } else {
                bannerViewTop.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bannerViewTop.setForegroundGravity(Gravity.CENTER);
            }
            ((ViewGroup) rootView).addView(bannerViewTop);
            sendBannerEvent("shown", "top", null);
        });
    }

    public static void removeBannerAd_Top() {
        if (mediationManager == null) return;

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            ViewGroup rootView = (ViewGroup) RunnerActivity.CurrentActivity
                    .findViewById(android.R.id.content).getRootView();
            if (bannerViewTop != null) {
                rootView.removeView(bannerViewTop);
                bannerViewTop.destroy();
                bannerViewTop = null;
                sendBannerEvent("destroyed", "top", null);
            }
        });
    }

    public static void showBannerAd_Big(double posX, double posY) {
        if (mediationManager == null || bannerViewBig != null) return;

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            ViewGroup rootView = (ViewGroup) RunnerActivity.CurrentActivity
                    .findViewById(android.R.id.content).getRootView();

            bannerViewBig = new CASBannerView(RunnerActivity.CurrentActivity, mediationManager);
            bannerViewBig.setSize(AdSize.MEDIUM_RECTANGLE);
            bannerViewBig.setAutoloadEnabled(true);
            bannerViewBig.setAdListener(makeBannerListener("big"));

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.leftMargin = (int) Math.round(posX);
            params.topMargin  = (int) Math.round(posY);
            bannerViewBig.setLayoutParams(params);

            rootView.addView(bannerViewBig);
            sendBannerEvent("shown", "big", null);
        });
    }

    public static void moveBannerAd_Big(double posX, double posY) {
        if (mediationManager == null) return;

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            if (bannerViewBig != null) {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.leftMargin = (int) Math.round(posX);
                params.topMargin  = (int) Math.round(posY);
                bannerViewBig.setLayoutParams(params);
            }
        });
    }

    public static void removeBannerAd_Big() {
        if (mediationManager == null) return;

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            ViewGroup rootView = (ViewGroup) RunnerActivity.CurrentActivity
                    .findViewById(android.R.id.content).getRootView();
            if (bannerViewBig != null) {
                rootView.removeView(bannerViewBig);
                bannerViewBig.destroy();
                bannerViewBig = null;
                sendBannerEvent("destroyed", "big", null);
            }
        });
    }

    public static void setBannerVisibility(double visibility) {
        if (mediationManager == null || bannerViewTop == null) return;

        RunnerActivity.CurrentActivity.runOnUiThread(() -> {
            int vis = (Math.round(visibility) == 1) ? View.VISIBLE : View.GONE;
            bannerViewTop.setVisibility(vis);
            sendBannerEvent(vis == View.VISIBLE ? "shown" : "hidden", "top", null);
        });
    }

    private static void sendBannerEvent(String state, String place, String errorOrNull) {
        int map = RunnerJNILib.jCreateDsMap(null, null, null);
        RunnerJNILib.DsMapAddString(map, "type", "cas_banner");
        RunnerJNILib.DsMapAddString(map, "state", state);    
        if (place != null) RunnerJNILib.DsMapAddString(map, "place", place); 
        if (errorOrNull != null) RunnerJNILib.DsMapAddString(map, "error", errorOrNull);
        RunnerJNILib.CreateAsynEventWithDSMap(map, EVENT_OTHER_SOCIAL);
    }
}
