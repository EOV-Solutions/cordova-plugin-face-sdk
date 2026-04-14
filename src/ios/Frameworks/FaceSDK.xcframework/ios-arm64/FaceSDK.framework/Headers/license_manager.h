#ifndef LICENSE_MANAGER_H
#define LICENSE_MANAGER_H

#include <string>
#include <mutex>
#include <cstdint>

namespace ppredictor {

/**
 * License status enumeration
 */
enum class LicenseStatus {
    NOT_INITIALIZED,
    VALID,
    EXPIRED,
    GRACE_PERIOD,
    INVALID,
    BLOCKED,
    QUOTA_EXCEEDED  // New status for quota block
};

/**
 * License information structure
 */
struct LicenseInfo {
    std::string device_id;
    std::string license_key;
    std::string org_id;
    std::string app_id;
    std::string status;
    int64_t exp;  // Expiration timestamp
    int64_t iat;  // Issued at timestamp
};

/**
 * LicenseManager - Singleton class for managing SDK license validation
 * 
 * Security is implemented at C++ layer to make reverse engineering harder.
 * JWT tokens are verified using HMAC-SHA256.
 * Quota management is also handled here for security.
 */
class LicenseManager {
public:
    /**
     * Get singleton instance
     */
    static LicenseManager* getInstance();
    
    /**
     * Initialize license with offline token
     * @param offline_token JWT token from server verification
     * @param device_id Current device ID
     * @return true if token is valid
     */
    bool initWithToken(const std::string& offline_token, const std::string& device_id);
    
    /**
     * Check if current license is valid (also checks quota block)
     * @return true if license is valid, not expired, and not blocked due to quota
     */
    bool isValid();
    
    /**
     * Get current license status
     */
    LicenseStatus getStatus();
    
    /**
     * Get license info (if valid)
     */
    const LicenseInfo& getLicenseInfo();
    
    /**
     * Clear license data
     */
    void clear();
    
    /**
     * Get the hardcoded API base URL (security: cannot be modified from Java)
     */
    static const char* getBaseUrl();

    // ==================== Quota Management (Security-critical) ====================
    
    /**
     * Increment usage counter. If threshold is reached, blocks the SDK.
     * @param amount Amount to increment (usually 1)
     * @return true if SDK is still usable, false if blocked due to quota
     */
    bool incrementUsage(int amount);
    
    /**
     * Check if SDK is blocked due to quota threshold exceeded
     * @return true if blocked
     */
    bool isBlockedDueToQuota();
    
    /**
     * Get the quota threshold value (hardcoded for security)
     * @return threshold value (1000)
     */
    int getQuotaThreshold();
    
    /**
     * Get current pending usage count
     * @return pending usage count
     */
    int getPendingUsage();
    
    /**
     * Set blocked state (called from Java after sync attempt)
     * @param blocked true to block, false to unblock
     */
    void setBlockedDueToQuota(bool blocked);
    
    /**
     * Reset pending usage count (called after successful sync)
     */
    void resetPendingUsage();
    
    /**
     * Update last sync time to current time
     */
    void updateLastSync();
    
    /**
     * Set last sync time (called from Java with stored value on startup)
     * @param timeMs last sync timestamp in milliseconds
     */
    void setLastSyncTime(int64_t timeMs);
    
    /**
     * Check if sync is needed based on time interval and quota
     * @return true if sync should be performed
     */
    bool shouldSync();
    
    /**
     * Get the sync interval value (hardcoded for security)
     * @return sync interval in milliseconds
     */
    int64_t getSyncInterval();

private:
    // API URL decoded at runtime to prevent extraction via `strings` command
    static std::string decodeBaseUrl();
    
    // Secret key decoded at runtime to prevent extraction via `strings` command
    static std::string decodeSecretKey();
    
    // Maximum allowed token lifetime: 400 days (in seconds)
    static constexpr int64_t MAX_TOKEN_LIFETIME = 400LL * 24 * 60 * 60;
    
    // Hardcoded quota threshold for security - cannot be changed from Java layer
    // static constexpr int QUOTA_THRESHOLD = 3;
    static constexpr int QUOTA_THRESHOLD = 3000;
    
    // Hardcoded sync interval for security - 15 day in milliseconds
    static constexpr int64_t DEFAULT_SYNC_INTERVAL = 1296000000L;
    // For testing: 60 seconds
    // static constexpr int64_t DEFAULT_SYNC_INTERVAL = 60000L;
    // For testing: 2 hours 
    // static constexpr int64_t DEFAULT_SYNC_INTERVAL = 7200000L;
    
    LicenseManager();
    ~LicenseManager() = default;
    
    // Prevent copying
    LicenseManager(const LicenseManager&) = delete;
    LicenseManager& operator=(const LicenseManager&) = delete;
    
    /**
     * Verify JWT token signature
     */
    bool verifyToken(const std::string& token);
    
    /**
     * Parse JWT payload
     */
    bool parseToken(const std::string& token);
    
    /**
     * Check if token is expired
     */
    bool isTokenExpired();
    
    static LicenseManager* instance_;
    static std::mutex mutex_;
    
    LicenseStatus status_;
    LicenseInfo license_info_;
    std::string secret_key_;
    std::string cached_token_;
    bool initialized_;
    
    // Quota management members
    int pending_usage_;
    bool blocked_due_to_quota_;
    int64_t last_sync_time_;  // Last sync timestamp in milliseconds
};

} // namespace ppredictor

#endif // LICENSE_MANAGER_H

