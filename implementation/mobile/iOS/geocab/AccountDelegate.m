//
//  AccountDelegate.m
//  geocab
//
//  Created by Henrique Lobato Zago on 14/10/14.
//  Copyright (c) 2014 Itaipu. All rights reserved.
//

#import "AccountDelegate.h"
#import "User.h"

@implementation AccountDelegate

- (RKObjectMapping *) mapping
{
    RKObjectMapping *mapping = [RKObjectMapping mappingForClass:[User class]];
    [mapping addAttributeMappingsFromDictionary:@{
                                                  @"id"          : @"id",
                                                  @"name"        : @"name",
                                                  @"created"     : @"created",
                                                  @"updated"     : @"updated",
                                                  @"email"       : @"email",
                                                  @"enabled"     : @"enabled",
                                                  @"role"        : @"role",
                                                  @"password"    : @"password",
                                                  @"authorities" : @"authorities"
                                                  }];
    
    return mapping;
}

- (void) loginWithEmail: (NSString *)email password: (NSString *)password successBlock: (void (^)(RKObjectRequestOperation *operation, RKMappingResult *result)) successBlock failureBlock: (void (^)(RKObjectRequestOperation *operation, NSError *error)) failureBlock
{
    NSString *credentials = [[email stringByAppendingString:@":"] stringByAppendingString:password];
    NSData *credentialsData = [credentials dataUsingEncoding:NSUTF8StringEncoding];
    NSString *credentialsEncoded = [credentialsData base64EncodedStringWithOptions:0];
    NSLog(@"credentials: %@", credentialsEncoded);
    
    NSDictionary *params = [[NSDictionary alloc] initWithObjectsAndKeys:credentialsEncoded, @"credentials", nil];
    
    RKResponseDescriptor *responseDescriptor = [RKResponseDescriptor responseDescriptorWithMapping:self.mapping method:RKRequestMethodPOST pathPattern:nil keyPath:nil statusCodes:nil];
    
    [[RKObjectManager sharedManager] addResponseDescriptor:responseDescriptor];
    [[RKObjectManager sharedManager] postObject:nil path:@"check" parameters:params success:successBlock failure:failureBlock];
}

@end

