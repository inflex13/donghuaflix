import Foundation
import Observation

@Observable
final class DetailViewModel {

    // MARK: - State

    var show: Show?
    var episodes: [Episode] = []
    var selectedWebsite: WebsiteInfo?
    var lastWatched: WatchProgress?
    var isInWatchlist = false
    var watchedEpisodes: [Int: WatchProgress] = [:]
    var preloadingEpisodes: Set<Int> = []
    var preloadedEpisodes: Set<Int> = []
    var isLoading = false
    var latestFirst = true
    var error: String?

    // Episode paging (for 50+ episodes)
    var currentPage = 0
    private let episodesPerPage = 50

    var totalPages: Int {
        max(1, Int(ceil(Double(sortedEpisodes.count) / Double(episodesPerPage))))
    }

    var sortedEpisodes: [Episode] {
        latestFirst ? episodes.sorted { $0.episodeNumber > $1.episodeNumber } : episodes.sorted { $0.episodeNumber < $1.episodeNumber }
    }

    var pagedEpisodes: [Episode] {
        let sorted = sortedEpisodes
        let start = currentPage * episodesPerPage
        let end = min(start + episodesPerPage, sorted.count)
        guard start < sorted.count else { return [] }
        return Array(sorted[start..<end])
    }

    var nextEpisodeNumber: Int? {
        if let lastWatched = lastWatched {
            if lastWatched.completed {
                return lastWatched.episodeNumber + 1
            }
            return lastWatched.episodeNumber
        }
        return episodes.min(by: { $0.episodeNumber < $1.episodeNumber })?.episodeNumber
    }

    private let showRepository: ShowRepository
    private let watchRepository: WatchRepository

    init(showRepository: ShowRepository, watchRepository: WatchRepository) {
        self.showRepository = showRepository
        self.watchRepository = watchRepository
    }

    // MARK: - Load

    @MainActor
    func loadShow(id: Int) async {
        guard !isLoading else { return }
        isLoading = true
        error = nil

        do {
            // Fetch show details
            show = try await showRepository.getShow(id: id)

            // Select first website by default
            if let firstWebsite = show?.websites.first, selectedWebsite == nil {
                selectedWebsite = firstWebsite
            }

            // Fetch episodes for selected website
            await loadEpisodes(showId: id)

            // Fetch watch history
            watchedEpisodes = try await watchRepository.getWatchedEpisodesForShow(showId: id)

            // Find last watched episode
            lastWatched = watchedEpisodes.values
                .sorted(by: { $0.episodeNumber > $1.episodeNumber })
                .first

            // Check watchlist
            isInWatchlist = watchRepository.isInWatchlist(showId: id)
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    @MainActor
    func selectWebsite(_ website: WebsiteInfo) async {
        guard website.id != selectedWebsite?.id else { return }
        selectedWebsite = website
        if let showId = show?.id {
            await loadEpisodes(showId: showId)
        }
    }

    @MainActor
    private func loadEpisodes(showId: Int) async {
        do {
            episodes = try await showRepository.getEpisodes(
                showId: showId,
                website: selectedWebsite?.name
            )
        } catch {
            self.error = error.localizedDescription
        }
    }

    // MARK: - Watch Actions

    @MainActor
    func markEpisodeWatched(_ episode: Episode) async {
        guard let showId = show?.id else { return }
        do {
            try await watchRepository.markAsWatched(showId: showId, episodeNumber: episode.episodeNumber, episodeId: episode.id)
            watchedEpisodes[episode.episodeNumber] = WatchProgress(
                showId: showId,
                episodeNumber: episode.episodeNumber,
                progressSeconds: 0,
                durationSeconds: 0,
                completed: true
            )
        } catch {
            // Silently fail
        }
    }

    @MainActor
    func markEpisodeUnwatched(_ episode: Episode) async {
        guard let showId = show?.id else { return }
        do {
            try await watchRepository.markAsUnwatched(showId: showId, episodeNumber: episode.episodeNumber, episodeId: episode.id)
            watchedEpisodes.removeValue(forKey: episode.episodeNumber)
        } catch {
            // Silently fail
        }
    }

    @MainActor
    func markAllWatchedUpTo(_ episodeNumber: Int) async {
        guard let showId = show?.id else { return }
        for episode in episodes where episode.episodeNumber <= episodeNumber {
            if watchedEpisodes[episode.episodeNumber]?.completed != true {
                do {
                    try await watchRepository.markAsWatched(showId: showId, episodeNumber: episode.episodeNumber, episodeId: episode.id)
                    watchedEpisodes[episode.episodeNumber] = WatchProgress(
                        showId: showId,
                        episodeNumber: episode.episodeNumber,
                        progressSeconds: 0,
                        durationSeconds: 0,
                        completed: true
                    )
                } catch {
                    continue
                }
            }
        }
    }

    @MainActor
    func toggleWatchlist() async {
        guard let showId = show?.id else { return }
        do {
            try await watchRepository.toggleWatchlist(showId: showId)
            isInWatchlist = watchRepository.isInWatchlist(showId: showId)
        } catch {
            // Silently fail
        }
    }

    // MARK: - Preload Sources

    @MainActor
    func preloadSources(for episode: Episode) async {
        guard !preloadingEpisodes.contains(episode.episodeNumber),
              !preloadedEpisodes.contains(episode.episodeNumber) else { return }

        preloadingEpisodes.insert(episode.episodeNumber)

        do {
            let sources = try await showRepository.getEpisodeSources(episodeId: episode.id)
            if !sources.isEmpty {
                preloadedEpisodes.insert(episode.episodeNumber)
            }
        } catch {
            // Silently fail
        }

        preloadingEpisodes.remove(episode.episodeNumber)
    }
}
