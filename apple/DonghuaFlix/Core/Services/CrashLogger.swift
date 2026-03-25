import Foundation
#if canImport(UIKit)
import UIKit
#endif

// MARK: - Crash Logger

final class CrashLogger {

    static let shared = CrashLogger()

    private var apiClient: APIClient?

    private init() {}

    func configure(apiClient: APIClient) {
        self.apiClient = apiClient
    }

    // MARK: - Log Levels

    func error(_ message: String, stacktrace: String? = nil, screen: String? = nil, extra: String? = nil) {
        log(level: "error", message: message, stacktrace: stacktrace, screen: screen, extra: extra)
    }

    func warning(_ message: String, screen: String? = nil, extra: String? = nil) {
        log(level: "warning", message: message, screen: screen, extra: extra)
    }

    func info(_ message: String, screen: String? = nil, extra: String? = nil) {
        log(level: "info", message: message, screen: screen, extra: extra)
    }

    // MARK: - Private

    private func log(level: String, message: String, stacktrace: String? = nil, screen: String? = nil, extra: String? = nil) {
        guard let apiClient else { return }

        let request = CrashLogRequest(
            level: level,
            message: message,
            stacktrace: stacktrace,
            appVersion: Self.appVersion,
            deviceInfo: Self.deviceInfo,
            screen: screen,
            extra: extra
        )

        Task {
            await apiClient.fireAndForget(.crashLog(request))
        }
    }

    // MARK: - Device Info

    static var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "unknown"
        return "\(version) (\(build))"
    }

    static var deviceInfo: String {
        #if os(iOS)
        let device = UIDevice.current
        let systemVersion = device.systemVersion
        let model = device.model
        let name = device.name
        return "\(model) - iOS \(systemVersion) - \(name)"
        #elseif os(macOS)
        let processInfo = ProcessInfo.processInfo
        let osVersion = processInfo.operatingSystemVersion
        return "macOS \(osVersion.majorVersion).\(osVersion.minorVersion).\(osVersion.patchVersion) - \(Self.macModelIdentifier)"
        #else
        return "Apple Platform - \(ProcessInfo.processInfo.operatingSystemVersionString)"
        #endif
    }

    #if os(macOS)
    private static var macModelIdentifier: String {
        var size = 0
        sysctlbyname("hw.model", nil, &size, nil, 0)
        var model = [CChar](repeating: 0, count: size)
        sysctlbyname("hw.model", &model, &size, nil, 0)
        return String(cString: model)
    }
    #endif
}
