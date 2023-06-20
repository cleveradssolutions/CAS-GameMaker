#import <CleverAdsSolutions/CleverAdsSolutions-Swift.h>

@interface CAS_GMwrapper : NSObject<CASCallback>
{
    @private
    CASMediationManager *mediationManager;
}

-(void) initialize:(NSString *)casId withAdTypes:(double)adTypes withTaggedAudience:(double)taggedAudience withConsentStatus:(double)consentStatus withCcpaStatus:(double)ccpaStatus isTestMode:(double)testMode;
-(double) showRewardedAd;

@end
