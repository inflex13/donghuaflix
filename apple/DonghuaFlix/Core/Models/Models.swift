import Foundation

// MARK: - Domain Models

struct Show: Identifiable, Hashable, Equatable {
    let id: Int
    let title: String
    let titleChinese: String?
    let posterUrl: String?
    let description: String?
    let rating: Double?
    let year: Int?
    let status: String?
    let genres: [String]
    let totalEpisodes: Int?
    let category: String?
    let websites: [WebsiteInfo]

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    static func == (lhs: Show, rhs: Show) -> Bool {
        lhs.id == rhs.id
    }
}

struct Episode: Identifiable, Hashable {
    let id: Int
    let episodeNumber: Int
    let title: String?
    let externalUrl: String?
    let websiteName: String?
    let hasSources: Bool
    let createdAt: String?

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

struct VideoSource: Identifiable, Hashable {
    let id: Int
    let sourceName: String
    let sourceKey: String
    let sourceUrl: String?
    let sourceType: String?
    let websiteName: String?
    let subtitles: [SubtitleTrack]

    var streamType: StreamType {
        StreamType.fromUrl(sourceUrl)
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

struct SubtitleTrack: Identifiable, Hashable {
    let language: String
    let label: String
    let url: String

    var id: String { language }
}

struct WatchProgress: Equatable {
    let showId: Int
    let episodeNumber: Int
    let progressSeconds: Int
    let durationSeconds: Int
    let completed: Bool

    var progressFraction: Double {
        guard durationSeconds > 0 else { return 0 }
        return Double(progressSeconds) / Double(durationSeconds)
    }

    var remainingSeconds: Int {
        max(0, durationSeconds - progressSeconds)
    }
}

struct WebsiteInfo: Identifiable, Hashable {
    let id: Int
    let name: String
    let displayName: String
    let episodeCount: Int?
}

struct HomeSection: Identifiable {
    let title: String
    let sectionType: String
    let shows: [Show]

    var id: String { title }
}

// MARK: - Stream Type

enum StreamType: String, Equatable {
    case hls
    case mp4
    case dash
    case unknown

    static func fromUrl(_ url: String?) -> StreamType {
        guard let url = url?.lowercased() else { return .unknown }

        if url.contains(".m3u8") || url.contains("m3u8") {
            return .hls
        } else if url.contains(".mp4") {
            return .mp4
        } else if url.contains(".mpd") {
            return .dash
        } else {
            return .unknown
        }
    }
}

// MARK: - DTO -> Domain Mappers

extension ShowDTO {
    func toDomain() -> Show {
        Show(
            id: id,
            title: title,
            titleChinese: titleChinese,
            posterUrl: posterUrl,
            description: description,
            rating: rating,
            year: year,
            status: status,
            genres: genres ?? [],
            totalEpisodes: totalEpisodes,
            category: category,
            websites: websites?.map { $0.toDomain() } ?? []
        )
    }
}

extension WebsiteInfoDTO {
    func toDomain() -> WebsiteInfo {
        WebsiteInfo(
            id: id,
            name: name,
            displayName: displayName,
            episodeCount: episodeCount
        )
    }
}

extension EpisodeDTO {
    func toDomain() -> Episode {
        Episode(
            id: id,
            episodeNumber: episodeNumber,
            title: title,
            externalUrl: externalUrl,
            websiteName: websiteName,
            hasSources: hasSources ?? true,
            createdAt: createdAt
        )
    }
}

extension SourceDTO {
    func toDomain() -> VideoSource {
        let subtitleTracks: [SubtitleTrack] = subtitles?.compactMap { (language, dto) in
            guard let url = dto.url, !url.isEmpty else { return nil }
            return SubtitleTrack(
                language: language,
                label: dto.label ?? language,
                url: url
            )
        } ?? []

        return VideoSource(
            id: id,
            sourceName: sourceName,
            sourceKey: sourceKey,
            sourceUrl: sourceUrl,
            sourceType: sourceType,
            websiteName: websiteName,
            subtitles: subtitleTracks
        )
    }
}

extension WatchProgressDTO {
    func toDomain() -> WatchProgress {
        WatchProgress(
            showId: showId,
            episodeNumber: episodeNumber,
            progressSeconds: progressSeconds ?? 0,
            durationSeconds: durationSeconds ?? 0,
            completed: completed ?? false
        )
    }
}

extension HomeSectionDTO {
    func toDomain() -> HomeSection {
        HomeSection(
            title: title,
            sectionType: sectionType,
            shows: shows.map { $0.toDomain() }
        )
    }
}
