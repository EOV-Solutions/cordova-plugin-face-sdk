//
//  LicenseManagerBridge.h
//  FaceSDK
//
//  Objective-C bridge to expose C++ LicenseManager to Swift
//
//  Copyright © 2026 EOV Solutions. All rights reserved.
//

#ifndef LicenseManagerBridge_h
#define LicenseManagerBridge_h

#import <Foundation/Foundation.h>

#ifdef __cplusplus
extern "C" {
#endif

// ==================== Configuration Constants (from C++) ====================

/// Get the hardcoded base URL from C++ layer
const char *FMLicenseGetBaseUrl(void);

/// Get the quota threshold value from C++ layer
int FMLicenseGetQuotaThreshold(void);

/// Get the sync interval in milliseconds from C++ layer
int64_t FMLicenseGetSyncInterval(void);

// ==================== License Initialization ======================================

/// Initialize license with offline token
/// @param token JWT token from server
/// @param deviceId Current device ID
/// @return YES if valid
BOOL FMLicenseInitWithToken(const char *token, const char *deviceId);

/// Check if license is valid
BOOL FMLicenseIsValid(void);

/// Get current license status (returns integer matching LicenseStatus enum)
int FMLicenseGetStatus(void);

/// Clear license data
void FMLicenseClear(void);

// ==================== License Info ====================

/// Get organization ID from license
const char *FMLicenseGetOrgId(void);

/// Get application ID from license
const char *FMLicenseGetAppId(void);

/// Get device ID from license
const char *FMLicenseGetDeviceId(void);

// ==================== Quota Management ====================

/// Increment usage counter
/// @param amount Amount to increment
/// @return YES if SDK is still usable, NO if blocked due to quota
BOOL FMLicenseIncrementUsage(int amount);

/// Check if blocked due to quota
BOOL FMLicenseIsBlockedDueToQuota(void);

/// Get pending usage count
int FMLicenseGetPendingUsage(void);

/// Set blocked state
void FMLicenseSetBlockedDueToQuota(BOOL blocked);

/// Reset pending usage (after successful sync)
void FMLicenseResetPendingUsage(void);

// ==================== Sync Management ====================

/// Check if sync is needed
BOOL FMLicenseShouldSync(void);

/// Update last sync time to now
void FMLicenseUpdateLastSync(void);

/// Set last sync time (from stored value)
void FMLicenseSetLastSyncTime(int64_t timeMs);

#ifdef __cplusplus
}
#endif

#endif /* LicenseManagerBridge_h */
