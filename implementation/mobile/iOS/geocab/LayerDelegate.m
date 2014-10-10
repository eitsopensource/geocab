//
//  LayerDelegate.m
//  geocab
//
//  Created by Henrique Lobato on 06/10/14.
//  Copyright (c) 2014 Itaipu. All rights reserved.
//

#import "LayerDelegate.h"
#import "Layer.h"

@implementation LayerDelegate

- (RKObjectMapping *) mapping
{
    RKObjectMapping *mapping = [RKObjectMapping mappingForClass:[Layer class]];
    [mapping addAttributeMappingsFromDictionary:@{
                                                  @"id"        : @"id",
                                                  @"name"      : @"name",
                                                  @"title"     : @"title",
                                                  @"legend"    : @"legend",
                                                  }];
    
    return mapping;
}

- (void) list: (void (^)(RKObjectRequestOperation *operation, RKMappingResult *result)) successBlock userName:(NSString*)userName password:(NSString*)password
{
    RKResponseDescriptor *responseDescriptor = [RKResponseDescriptor responseDescriptorWithMapping:self.mapping method:RKRequestMethodGET pathPattern:nil keyPath:@"" statusCodes:nil];
    
    NSURL* url = [NSURL URLWithString:self.baseUrl];
    RKObjectManager* objectManager = [RKObjectManager managerWithBaseURL:url];
    [objectManager.HTTPClient setAuthorizationHeaderWithUsername:userName password:password];
    
    NSURLRequest *request = [objectManager requestWithObject:nil method:RKRequestMethodGET path:@"" parameters:nil];
    
    RKObjectRequestOperation *objectRequestOperation = [[RKObjectRequestOperation alloc] initWithRequest:request responseDescriptors:@[ responseDescriptor ]];
    
    [objectRequestOperation setCompletionBlockWithSuccess:successBlock failure:nil];
    [objectRequestOperation start];
}

@end