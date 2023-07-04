#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(Sslpinning, NSObject)

RCT_EXTERN_METHOD(fetch:(NSString)url withData:(NSDictionary)data withCallback:(RCTResponseSenderBlock)callback)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
