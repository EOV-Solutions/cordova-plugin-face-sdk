#ifndef JWT_UTILS_H
#define JWT_UTILS_H

#include <string>
#include <vector>
#include <cstdint>

namespace ppredictor {
namespace jwt {

/**
 * Base64 URL-safe decode
 * @param input Base64 URL-safe encoded string
 * @return Decoded bytes as string
 */
std::string base64UrlDecode(const std::string& input);

/**
 * HMAC-SHA256 computation
 * @param key Secret key
 * @param data Data to sign
 * @return HMAC result as bytes
 */
std::vector<uint8_t> hmacSha256(const std::string& key, const std::string& data);

/**
 * Split JWT into parts (header.payload.signature)
 * @param token JWT token string
 * @return Vector of 3 parts, empty if invalid format
 */
std::vector<std::string> splitToken(const std::string& token);

/**
 * Simple JSON value extraction (no external dependency)
 * @param json JSON string
 * @param key Key to extract
 * @return Value as string, empty if not found
 */
std::string getJsonString(const std::string& json, const std::string& key);

/**
 * Simple JSON integer extraction
 * @param json JSON string
 * @param key Key to extract
 * @return Value as int64, 0 if not found
 */
int64_t getJsonInt(const std::string& json, const std::string& key);

/**
 * Get current Unix timestamp
 */
int64_t getCurrentTimestamp();

} // namespace jwt
} // namespace ppredictor

#endif // JWT_UTILS_H
