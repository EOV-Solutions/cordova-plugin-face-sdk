/**
 * TypeScript definitions for cordova-plugin-face-sdk
 */

interface InitializeOptions {
    /** License key from server */
    licenseKey: string;
    /** Optional face ID (used as userId in registration) */
    faceId?: string;
    /** Optional user name (used in registration) */
    userName?: string;
}

interface InitializeResult {
    success: boolean;
    message?: string;
    orgId?: string;
    error?: string;
}

interface LicenseResult {
    valid: boolean;
    status?: number;
    message?: string;
}

interface LicenseInfo {
    isValid: boolean;
    status: number;
    message?: string;
    expiryDate?: string;
    maxUsers?: number;
    currentUsers?: number;
    features?: string[];
}

interface PermissionResult {
    granted: boolean;
    status: 'granted' | 'denied' | 'restricted' | 'undetermined' | 'unknown';
}

interface RegistrationOptions {
    /** Optional user ID (uses faceId from initialize if not provided) */
    userId?: string;
    /** Optional user display name */
    userName?: string;
    /** Skip name dialog and use provided userName directly */
    skipNameDialog?: boolean;
}

interface RegistrationResult {
    success: boolean;
    userId?: string;
    userName?: string;
    orgId?: string;
    featureCount?: number;
    serverSynced?: boolean;
    error?: string;
}

interface RecognitionOptions {
    /** Recognition timeout in seconds (default: 30) */
    timeoutSeconds?: number;
    /** Recognition mode: 'verify' (1:1, default) | 'identify' (1:N) */
    mode?: 'identify' | 'verify';
    /** User ID to verify against — required when mode is 'verify' */
    userId?: string;
}

interface RecognitionResult {
    success: boolean;
    /** Whether the face is live (not a photo/video) */
    isLive: boolean;
    /** Whether a user was recognized */
    isRecognized: boolean;
    /** User ID if recognized */
    userId?: string;
    /** User name if recognized */
    userName?: string;
    /** Recognition confidence score (0-1) */
    confidence?: number;
    /** Path to captured face image */
    imagePath?: string;
    error?: string;
}

interface RefreshResult {
    success: boolean;
    /** Number of users deleted from local DB */
    deletedCount: number;
    /** User ID of re-imported user */
    userId: string;
    /** User name of re-imported user */
    userName: string;
    message?: string;
}

interface TerminateResult {
    success: boolean;
    message?: string;
}

interface DeviceCompatibilityResult {
    /** Overall compatibility - true if all critical checks pass */
    compatible: boolean;
    /** Human-readable summary message */
    message: string;
    /** Platform: 'android' or 'ios' */
    platform: 'android' | 'ios';
    /** OS version string (e.g. '14.0', '33') */
    osVersion: string;
    /** Device model (e.g. 'iPhone 12', 'SM-G991B') */
    deviceModel: string;
    /** Detailed check results */
    checks: {
        /** Device has a front-facing camera */
        hasFrontCamera: boolean;
        /** OS version meets minimum requirement (Android 7+ / iOS 12+) */
        osVersionSupported: boolean;
        /** CPU architecture supports arm64 (required for AI models) */
        cpuArchSupported: boolean;
        /** Device has enough RAM (>= 2GB) */
        hasEnoughRAM: boolean;
        /** Device has enough free storage (>= 100MB) */
        hasEnoughStorage: boolean;
    };
    /** RAM in MB */
    totalRAM: number;
    /** Available storage in MB */
    availableStorage: number;
    /** List of reasons if not compatible */
    unsupportedReasons: string[];
}

interface FaceSDKPromise {
    initialize(options: InitializeOptions): Promise<InitializeResult>;
    isLicenseValid(): Promise<LicenseResult>;
    getLicenseInfo(): Promise<LicenseInfo>;
    getLicenseStatus(): Promise<number>;
    startRegistration(options?: RegistrationOptions): Promise<RegistrationResult>;
    isUserEnrolled(userId: string): Promise<boolean>;
    deleteUser(userId: string): Promise<boolean>;
    refreshEmbeddings(faceId?: string): Promise<RefreshResult>;
    startRecognition(options?: RecognitionOptions): Promise<RecognitionResult>;
    checkPermission(): Promise<PermissionResult>;
    requestPermission(): Promise<PermissionResult>;
    terminate(): Promise<TerminateResult>;
    isInitialized(): Promise<boolean>;
    checkDeviceCompatibility(): Promise<DeviceCompatibilityResult>;
}

interface FaceSDKPlugin {
    // Callback-style API
    initialize(options: InitializeOptions, success: (result: InitializeResult) => void, error: (err: any) => void): void;
    isLicenseValid(success: (result: LicenseResult) => void, error: (err: any) => void): void;
    getLicenseInfo(success: (result: LicenseInfo) => void, error: (err: any) => void): void;
    getLicenseStatus(success: (status: number) => void, error: (err: any) => void): void;
    startRegistration(options: RegistrationOptions, success: (result: RegistrationResult) => void, error: (err: any) => void): void;
    startRegistration(success: (result: RegistrationResult) => void, error: (err: any) => void): void;
    isUserEnrolled(userId: string, success: (enrolled: boolean) => void, error: (err: any) => void): void;
    deleteUser(userId: string, success: (deleted: boolean) => void, error: (err: any) => void): void;
    refreshEmbeddings(faceId: string, success: (result: RefreshResult) => void, error: (err: any) => void): void;
    refreshEmbeddings(success: (result: RefreshResult) => void, error: (err: any) => void): void;
    startRecognition(options: RecognitionOptions, success: (result: RecognitionResult) => void, error: (err: any) => void): void;
    startRecognition(success: (result: RecognitionResult) => void, error: (err: any) => void): void;
    checkPermission(success: (result: PermissionResult) => void, error: (err: any) => void): void;
    requestPermission(success: (result: PermissionResult) => void, error: (err: any) => void): void;
    checkCameraPermission(success: (result: PermissionResult) => void, error: (err: any) => void): void;
    requestCameraPermission(success: (result: PermissionResult) => void, error: (err: any) => void): void;
    terminate(success: (result: TerminateResult) => void, error: (err: any) => void): void;
    isInitialized(success: (initialized: boolean) => void, error: (err: any) => void): void;
    checkDeviceCompatibility(success: (result: DeviceCompatibilityResult) => void, error: (err: any) => void): void;

    /** Promise-based API wrapper */
    promise: FaceSDKPromise;
}

declare var FaceSDK: FaceSDKPlugin;
