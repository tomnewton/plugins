// Copyright 2017, the Flutter project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import <Flutter/Flutter.h>
#import "GoogleMobileAds/GoogleMobileAds.h"

typedef enum : NSUInteger {
  CREATED,
  LOADING,
  FAILED,
  PENDING,  // Will be shown when status is changed to LOADED.
  LOADED,
} MobileAdStatus;

@interface MobileAd : NSObject
+ (void)configureWithAppId:(NSString *)appId;
+ (MobileAd *)getAdForId:(NSNumber *)mobileAdId;
- (MobileAdStatus)status;
- (void)loadWithUnitId:(NSString *)unitId targetingInfo:(NSDictionary *)targetingInfo;
- (void)show;
- (void)dispose;
@end

@interface BannerAd : MobileAd<GADBannerViewDelegate>
+ (instancetype)withId:(NSNumber *)mobileAdId channel:(FlutterMethodChannel *)channel;
@end

@interface InterstitialAd : MobileAd<GADInterstitialDelegate>
+ (instancetype)withId:(NSNumber *)mobileAdId channel:(FlutterMethodChannel *)channel;
@end
