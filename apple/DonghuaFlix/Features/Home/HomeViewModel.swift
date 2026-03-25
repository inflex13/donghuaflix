import Foundation
import Observation

@Observable
final class HomeViewModel {

    var isLoading = false
    var sections: [HomeSection] = []
    var continueWatching: [(Show, WatchProgress)] = []
    var watchlistIds: Set<Int> = []
    var error: String?

    private let showRepository: ShowRepository
    private let watchRepository: WatchRepository

    init(showRepository: ShowRepository, watchRepository: WatchRepository) {
        self.showRepository = showRepository
        self.watchRepository = watchRepository
    }

    // MARK: - Load

    @MainActor
    func loadHome() async {
        guard !isLoading else { return }
        isLoading = true
        error = nil

        do {
            var builtSections: [HomeSection] = []

            // Recently Added — per website (like Android TV)
            async let dfTask = showRepository.getShows(page: 1, pageSize: 20, website: "donghuafun")
            async let akTask = showRepository.getShows(page: 1, pageSize: 20, website: "animekhor")
            async let continueTask = watchRepository.getContinueWatching()
            async let watchlistTask: Void = watchRepository.refreshWatchlistCache()

            let (dfResult, akResult, fetchedContinue, _) = try await (dfTask, akTask, continueTask, watchlistTask)

            if !dfResult.items.isEmpty {
                builtSections.append(HomeSection(
                    title: "DonghuaFun",
                    sectionType: "donghuafun",
                    shows: dfResult.items.map { $0.toDomain() }
                ))
            }

            if !akResult.items.isEmpty {
                builtSections.append(HomeSection(
                    title: "AnimeKhor",
                    sectionType: "animekhor",
                    shows: akResult.items.map { $0.toDomain() }
                ))
            }

            // Genre rows
            do {
                let genres = try await showRepository.getAllGenres()
                for genre in genres.prefix(4) {
                    let genreResult = try await showRepository.getShows(page: 1, pageSize: 20, genre: genre)
                    if !genreResult.items.isEmpty {
                        builtSections.append(HomeSection(
                            title: genre,
                            sectionType: "genre",
                            shows: genreResult.items.map { $0.toDomain() }
                        ))
                    }
                }
            } catch { /* genre rows are optional */ }

            // Completed series
            do {
                let completedResult = try await showRepository.getShows(page: 1, pageSize: 20, status: "completed")
                if !completedResult.items.isEmpty {
                    builtSections.append(HomeSection(
                        title: "Completed Series",
                        sectionType: "completed",
                        shows: completedResult.items.map { $0.toDomain() }
                    ))
                }
            } catch { /* optional */ }

            sections = builtSections
            continueWatching = fetchedContinue
            watchlistIds = watchRepository.watchlistIds
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    // MARK: - Full Resync

    @MainActor
    func fullResync() async {
        await loadHome()
    }
}
