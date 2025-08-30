#import "CAS_GMwrapper.h"

static int const EVENT_OTHER_SOCIAL = 70;
static int const ASYNC_RESPONSE_INITIALIZED = 324400;
static int const ASYNC_RESPONSE_NOT_INITIALIZED = 324401;
static int const ASYNC_RESPONSE_ON_REWARD = 324402;

extern "C" int dsMapCreate();
extern "C" void dsMapAddInt(int _dsMap, char* _key, int _value);
extern "C" void dsMapAddString(int _dsMap, char* _key, char* _value);
extern "C" void CreateAsyncEventOfTypeWithDSMap(int dsmapindex, int event_index);



@interface RewardedCallback : NSObject<CASCallback>
@end

@implementation RewardedCallback

-(void) didCompletedAd {
    int rewardCallback = dsMapCreate();
    dsMapAddInt(rewardCallback, (char *)"id", ASYNC_RESPONSE_ON_REWARD);
    CreateAsyncEventOfTypeWithDSMap(rewardCallback, EVENT_OTHER_SOCIAL);
}

@end



@implementation CAS_GMwrapper

//static int const AD_TYPE_BANNER = 1 << 0;
static int const AD_TYPE_INTERSTITIAL = 1 << 1;
static int const AD_TYPE_REWARDED = 1 << 2;

static int const AUDIENCE_UNDEFINED = 0;
static int const AUDIENCE_CHILDREN = 1;
static int const AUDIENCE_NOT_CHILDREN = 2;

static int const CONSENT_STATUS_UNDEFINED = 0;
static int const CONSENT_STATUS_ACCEPTED = 1;
static int const CONSENT_STATUS_DENIED = 2;

static int const CCPA_STATUS_UNDEFINED = 0;
static int const CCPA_STATUS_OPT_OUT_SALE = 1;
static int const CCPA_STATUS_OPT_IN_SALE = 2;

extern UIViewController *g_controller;

RewardedCallback *rewardedCallback;
CASBannerView *bannerView;

-(void) positionBannerViewAtBottomOfSafeArea:(UIView *)bannerView {
    // Проверка на наличие безопасной зоны
    UILayoutGuide *guide = g_controller.view.safeAreaLayoutGuide;
    
    // Создание ограничений для позиционирования баннера
    [NSLayoutConstraint activateConstraints:@[
        [bannerView.centerXAnchor constraintEqualToAnchor:guide.centerXAnchor],  // Центр по горизонтали
        [bannerView.bottomAnchor constraintEqualToAnchor:guide.bottomAnchor]     // Прикрепление к нижней части safeArea
    ]];
}

-(void) initialize:(NSString *)casId withAdTypes:(double)adTypes withTaggedAudience:(double)taggedAudience withConsentStatus:(double)consentStatus withCcpaStatus:(double)ccpaStatus isTestMode:(double)testMode {
    
    int adTypesInt = (int)lround(adTypes);
    CASTypeFlags casAdTypes = CASTypeFlagsNone;
    if (adTypesInt & AD_TYPE_INTERSTITIAL)
        casAdTypes |= CASTypeFlagsInterstitial;
    if (adTypesInt & AD_TYPE_REWARDED)
        casAdTypes |= CASTypeFlagsRewarded;

    //tagged audience
    switch (lround(taggedAudience)) {
        case AUDIENCE_UNDEFINED:
            [CAS.settings setTaggedWithAudience:CASAudienceUndefined];
            break;
        case AUDIENCE_CHILDREN:
            [CAS.settings setTaggedWithAudience:CASAudienceChildren];
            break;
        case AUDIENCE_NOT_CHILDREN:
            [CAS.settings setTaggedWithAudience:CASAudienceNotChildren];
            break;
    }
    
    //GDPR consent status
    switch (lround(consentStatus)) {
        case CONSENT_STATUS_UNDEFINED:
            [CAS.settings updateUserWithConsent:CASConsentStatusUndefined];
            break;
        case CONSENT_STATUS_ACCEPTED:
            [CAS.settings updateUserWithConsent:CASConsentStatusAccepted];
            break;
        case CONSENT_STATUS_DENIED:
            [CAS.settings updateUserWithConsent:CASConsentStatusDenied];
            break;
    }
    
    //CCPA status
    switch (lround(ccpaStatus)) {
        case CCPA_STATUS_UNDEFINED:
            [CAS.settings updateCCPAWithStatus:CASCCPAStatusUndefined];
            break;
        case CCPA_STATUS_OPT_OUT_SALE:
            [CAS.settings updateCCPAWithStatus:CASCCPAStatusOptOutSale];
            break;
        case CCPA_STATUS_OPT_IN_SALE:
            [CAS.settings updateCCPAWithStatus:CASCCPAStatusOptInSale];
            break;
    }
    
    rewardedCallback = [[RewardedCallback alloc] init];
    
    CASManagerBuilder *builder =  [CAS buildManager];
    [builder withAdFlags:CASTypeFlagsBanner | CASTypeFlagsInterstitial | CASTypeFlagsRewarded];
    [builder withTestAdMode:testMode == 1 ? YES : NO];
    [builder withCompletionHandler:^(CASInitialConfig * _Nonnull config) {
        int initCallback = dsMapCreate();
        if (config.error) {
            dsMapAddInt(initCallback, (char *)"id", ASYNC_RESPONSE_NOT_INITIALIZED);
            dsMapAddString(initCallback, (char *)"error", (char *)[config.error UTF8String]);
        }
        else
            dsMapAddInt(initCallback, (char *)"id", ASYNC_RESPONSE_INITIALIZED);
        CreateAsyncEventOfTypeWithDSMap(initCallback, EVENT_OTHER_SOCIAL);
    }];
    mediationManager = [builder createWithCasId:casId];
}

// Показываем баннер
-(void) showBanner {
    if (!bannerView) {
        // Получаем адаптивный размер баннера
        CASSize *adSize = [CASSize getAdaptiveBannerInWindow:g_controller.view.window];
        
        // Инициализация баннера
        bannerView = [[CASBannerView alloc] initWithAdSize:adSize manager:mediationManager];
        bannerView.rootViewController = g_controller;
        [bannerView setTranslatesAutoresizingMaskIntoConstraints:NO];
        
        // Добавляем баннер на экран
        [g_controller.view addSubview:bannerView];
        [self positionBannerViewAtBottomOfSafeArea:bannerView];
    }
}

// Удаляем баннер и освобождаем память
-(void) removeBanner {
    if (bannerView) {
        [bannerView removeFromSuperview];
        bannerView = nil; // Освобождаем память
    }
}

-(double) showRewardedAd {
    if (mediationManager) {
        if (mediationManager.isRewardedAdReady) {
            [mediationManager presentRewardedAdFromRootViewController:g_controller callback:rewardedCallback];
            return 1;
        }
        else
            return 0;
    }
    else
        return 0;
}

@end
