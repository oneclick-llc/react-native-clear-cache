#import "ClearCache.h"

#ifndef CACHE_CLEAR_IGNORE_PATHS
#define CACHE_CLEAR_IGNORE_PATHS [NSArray arrayWithObjects: @"ru.yandex.mobile.YandexMobileMetrica",@"crashes",@"com.plausiblelabs.crashreporter",nil]
#endif

@implementation ClearCache

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(getAppCacheSize:(RCTResponseSenderBlock)callback)
{
    NSString* fileSize = [self filePath:@"2"];
    NSString* fileSizeName = [self filePath:@"1"];
    callback(@[fileSize, fileSizeName]);
}

RCT_EXPORT_METHOD(clearAppCache:(RCTResponseSenderBlock)callback)
{
    [self clearFile:callback];
}

- (NSString*)filePath:(NSString*)type
{
    NSString * cachPath = [ NSSearchPathForDirectoriesInDomains ( NSCachesDirectory , NSUserDomainMask , YES ) firstObject ];
    return [self folderSizeAtPath :cachPath type:type];
}

- (long long)fileSizeAtPath:( NSString *) filePath {
    NSFileManager * manager = [ NSFileManager defaultManager];
    if ([manager fileExistsAtPath :filePath]) {
        return [[manager attributesOfItemAtPath :filePath error : nil ] fileSize ];
    }
    return 0;
}

- (NSString*)folderSizeAtPath:(NSString *) folderPath type:(NSString*)type {
    
    NSFileManager *manager = [NSFileManager defaultManager];
    
    if (![manager fileExistsAtPath :folderPath]) return 0 ;
    
    NSEnumerator *childFilesEnumerator = [[manager subpathsAtPath :folderPath] objectEnumerator ];
    
    NSString *fileName;
    
    long long folderSize = 0 ;
    
    while ((fileName = [childFilesEnumerator nextObject ]) != nil ) {
        NSString * fileAbsolutePath = [folderPath stringByAppendingPathComponent :fileName];
        folderSize += [ self fileSizeAtPath :fileAbsolutePath];
    }
    
    NSNumberFormatter *formatter = [[NSNumberFormatter alloc] init];
    formatter.roundingMode = NSNumberFormatterRoundFloor;
    formatter.maximumFractionDigits = 2;
    
    NSString* strFileSize = [[NSString alloc]init];
    NSMutableString* strFileName = [[NSMutableString alloc]init];
    if (folderSize < 1024) {
        NSNumber* fileSize = [NSNumber numberWithFloat: folderSize];
        strFileSize = [formatter stringFromNumber:fileSize];
        [strFileName setString:@"B"];
    } else if (folderSize < 1048576) {
        NSNumber* fileSize = [NSNumber numberWithFloat: (folderSize / 1024.0)];
        strFileSize = [formatter stringFromNumber:fileSize];
        [strFileName setString:@"KB"];
    } else if(folderSize < 1073741824) {
        NSNumber* fileSize = [NSNumber numberWithFloat: (folderSize / 1048576.0)];
        strFileSize = [formatter stringFromNumber:fileSize];
        [strFileName setString:@"MB"];
    } else {
        NSNumber* fileSize = [NSNumber numberWithFloat: (folderSize / 1073741824.0)];
        strFileSize = [formatter stringFromNumber:fileSize];
        [strFileName setString:@"G"];
    }
    
    if ([type isEqualToString:@"1"]) {
        return strFileName;
    } else {
        return strFileSize;
    }
}

- (void)clearFile:(RCTResponseSenderBlock)callback
{
    [[NSURLCache sharedURLCache] removeAllCachedResponses];
    NSLog(@"success clearing NSURLCache");
    
    NSArray *ignorePaths = CACHE_CLEAR_IGNORE_PATHS;
    NSString * cachPath = [NSSearchPathForDirectoriesInDomains (NSCachesDirectory, NSUserDomainMask, YES ) firstObject];
    
    NSArray * files = [[NSFileManager defaultManager]subpathsAtPath:cachPath];
    
    NSLog ( @"cache path = %@" , cachPath);
    NSLog ( @"cache files = %@" , files);
    
    for ( NSString * p in files) {
        
        NSError * error = nil ;
        NSString * path = [cachPath stringByAppendingPathComponent :p];
        
        BOOL ignore = false;
        for (NSString *ignorePath in ignorePaths) {
            if ([p hasPrefix: ignorePath]) {
                ignore = true;
                NSLog(@"skip removing file because it's ignored %@", p);
            }
        }
        if (ignore) continue;
        
        BOOL isDir;
        if (  [[ NSFileManager defaultManager ] fileExistsAtPath :path isDirectory: &isDir] && !isDir) {
            [[ NSFileManager defaultManager ] removeItemAtPath :path error :&error];
            
            if (error) {
                NSLog(@"error removing file %@: %@", p, error.localizedDescription);
            } else {
                NSLog(@"success removing file %@", p);
            }
        }
    }
    callback(@[[NSNull null]]);
    
}

@end
