import Foundation

// MARK: - Show DTOs

struct ShowDTO: Codable, Identifiable, Equatable {
    let id: Int
    let title: String
    let titleChinese: String?
    let slug: String?
    let posterUrl: String?
    let description: String?
    let rating: Double?
    let year: Int?
    let status: String?
    let genres: [String]?
    let totalEpisodes: Int?
    let category: String?
    let createdAt: String?
    let updatedAt: String?
    let remoteUpdatedAt: String?
    let websites: [WebsiteInfoDTO]?

    static func == (lhs: ShowDTO, rhs: ShowDTO) -> Bool {
        lhs.id == rhs.id
    }
}

struct WebsiteInfoDTO: Codable, Identifiable, Equatable {
    let id: Int
    let name: String
    let displayName: String
    let episodeCount: Int?
}

struct ShowListDTO: Codable {
    let items: [ShowDTO]
    let total: Int
    let page: Int
    let pageSize: Int
}

// MARK: - Episode DTOs

struct EpisodeDTO: Codable, Identifiable, Equatable {
    let id: Int
    let episodeNumber: Int
    let title: String?
    let externalUrl: String?
    let websiteName: String?
    let hasSources: Bool?
    let createdAt: String?

    static func == (lhs: EpisodeDTO, rhs: EpisodeDTO) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Source DTOs

struct SubtitleDTO: Codable, Equatable {
    let label: String?
    let url: String?
}

struct SourceDTO: Codable, Identifiable, Equatable {
    let id: Int
    let sourceName: String
    let sourceKey: String
    let sourceUrl: String?
    let sourceType: String?
    let websiteName: String?
    let subtitles: [String: SubtitleDTO]?

    static func == (lhs: SourceDTO, rhs: SourceDTO) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Watch Progress DTOs

struct WatchProgressDTO: Codable, Equatable {
    let id: Int?
    let showId: Int
    let episodeNumber: Int
    let progressSeconds: Int?
    let durationSeconds: Int?
    let completed: Bool?
    let watchedAt: String?
    let episodeId: Int?
}

// MARK: - Sync DTOs

struct SyncResponseDTO: Codable {
    let shows: [ShowDTO]
    let watchHistory: [WatchProgressDTO]
    let watchlist: [Int]
    let timestamp: String
}

// MARK: - Home DTOs

struct HomeSectionDTO: Codable {
    let title: String
    let sectionType: String
    let shows: [ShowDTO]
}

struct HomeResponseDTO: Codable {
    let sections: [HomeSectionDTO]
}

// MARK: - App Version DTO

struct AppVersionDTO: Codable {
    let versionCode: Int
    let versionName: String
    let downloadUrl: String
    let apkSize: Int?
    let changelog: String?
}

// MARK: - Request DTOs

struct CrashLogRequest: Codable {
    let level: String
    let message: String
    let stacktrace: String?
    let appVersion: String
    let deviceInfo: String
    let screen: String?
    let extra: String?
}

struct WatchProgressRequest: Codable {
    let showId: Int
    let episodeNumber: Int
    let progressSeconds: Int
    let durationSeconds: Int
    let completed: Bool
    let episodeId: Int?
}
