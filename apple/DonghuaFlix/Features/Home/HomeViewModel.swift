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
            async let sectionsTask = showRepository.getHomeSections()
            async let continueTask = watchRepository.getContinueWatching()
            async let watchlistTask: Void = watchRepository.refreshWatchlistCache()

            let (fetchedSections, fetchedContinue, _) = try await (sectionsTask, continueTask, watchlistTask)

            sections = fetchedSections
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
        isLoading = true
        error = nil

        do {
            async let sectionsTask = showRepository.getHomeSections()
            async let continueTask = watchRepository.getContinueWatching()
            async let watchlistTask: Void = watchRepository.refreshWatchlistCache()

            let (fetchedSections, fetchedContinue, _) = try await (sectionsTask, continueTask, watchlistTask)

            sections = fetchedSections
            continueWatching = fetchedContinue
            watchlistIds = watchRepository.watchlistIds
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }
}
