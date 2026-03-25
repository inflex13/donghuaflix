import Foundation
import Observation

@Observable
final class WatchRepository {

    private let apiClient: APIClient

    /// Local cache of watchlist show IDs for quick lookups
    private(set) var watchlistIds: Set<Int> = []

    init(apiClient: APIClient) {
        self.apiClient = apiClient
    }

    // MARK: - Watch Progress

    func updateProgress(
        showId: Int,
        episodeNumber: Int,
        progressSeconds: Int,
        durationSeconds: Int,
        completed: Bool,
        episodeId: Int? = nil
    ) async throws {
        let request = WatchProgressRequest(
            showId: showId,
            episodeNumber: episodeNumber,
            progressSeconds: progressSeconds,
            durationSeconds: durationSeconds,
            completed: completed,
            episodeId: episodeId
        )
        let _: WatchProgressDTO = try await apiClient.request(.updateProgress(request))
    }

    func markAsWatched(showId: Int, episodeNumber: Int, episodeId: Int? = nil) async throws {
        let request = WatchProgressRequest(
            showId: showId,
            episodeNumber: episodeNumber,
            progressSeconds: 0,
            durationSeconds: 0,
            completed: true,
            episodeId: episodeId
        )
        let _: WatchProgressDTO = try await apiClient.request(.updateProgress(request))
    }

    func markAsUnwatched(showId: Int, episodeNumber: Int, episodeId: Int? = nil) async throws {
        let request = WatchProgressRequest(
            showId: showId,
            episodeNumber: episodeNumber,
            progressSeconds: 0,
            durationSeconds: 0,
            completed: false,
            episodeId: episodeId
        )
        let _: WatchProgressDTO = try await apiClient.request(.updateProgress(request))
    }

    // MARK: - Continue Watching

    func getContinueWatching() async throws -> [(Show, WatchProgress)] {
        // Get continue watching progress entries
        let progressDTOs: [WatchProgressDTO] = try await apiClient.request(.continueWatching)

        // We need to fetch show info for each progress entry
        var results: [(Show, WatchProgress)] = []
        for progressDTO in progressDTOs {
            let progress = progressDTO.toDomain()
            // Fetch the show for each progress entry
            do {
                let showDTO: ShowDTO = try await apiClient.request(.show(id: progress.showId))
                results.append((showDTO.toDomain(), progress))
            } catch {
                // Skip entries where the show can't be fetched
                continue
            }
        }
        return results
    }

    // MARK: - Watch History

    func getWatchHistory(limit: Int = 50) async throws -> [WatchProgress] {
        let dtos: [WatchProgressDTO] = try await apiClient.request(.watchHistory(limit: limit))
        return dtos.map { $0.toDomain() }
    }

    func getWatchedEpisodesForShow(showId: Int) async throws -> [Int: WatchProgress] {
        let allHistory = try await getWatchHistory(limit: 500)
        var episodeMap: [Int: WatchProgress] = [:]
        for progress in allHistory where progress.showId == showId {
            episodeMap[progress.episodeNumber] = progress
        }
        return episodeMap
    }

    // MARK: - Watchlist

    func getWatchlist() async throws -> [Show] {
        let dtos: [ShowDTO] = try await apiClient.request(.watchlist)
        let shows = dtos.map { $0.toDomain() }
        watchlistIds = Set(shows.map { $0.id })
        return shows
    }

    func addToWatchlist(showId: Int) async throws {
        try await apiClient.requestVoid(.addToWatchlist(showId: showId))
        watchlistIds.insert(showId)
    }

    func removeFromWatchlist(showId: Int) async throws {
        try await apiClient.requestVoid(.removeFromWatchlist(showId: showId))
        watchlistIds.remove(showId)
    }

    func toggleWatchlist(showId: Int) async throws {
        if isInWatchlist(showId: showId) {
            try await removeFromWatchlist(showId: showId)
        } else {
            try await addToWatchlist(showId: showId)
        }
    }

    func isInWatchlist(showId: Int) -> Bool {
        watchlistIds.contains(showId)
    }

    /// Call this on launch to populate the local watchlist cache
    func refreshWatchlistCache() async {
        do {
            _ = try await getWatchlist()
        } catch {
            // Silently fail — cache will be populated on next successful call
        }
    }
}
