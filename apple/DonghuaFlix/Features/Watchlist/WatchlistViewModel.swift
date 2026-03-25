import Foundation
import Observation

@Observable
final class WatchlistViewModel {

    var shows: [Show] = []
    var isLoading = false
    var error: String?

    private let watchRepository: WatchRepository

    init(watchRepository: WatchRepository) {
        self.watchRepository = watchRepository
    }

    // MARK: - Load

    @MainActor
    func loadWatchlist() async {
        guard !isLoading else { return }
        isLoading = true
        error = nil

        do {
            shows = try await watchRepository.getWatchlist()
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    // MARK: - Remove

    @MainActor
    func removeFromWatchlist(showId: Int) async {
        do {
            try await watchRepository.removeFromWatchlist(showId: showId)
            shows.removeAll { $0.id == showId }
        } catch {
            // Silently fail
        }
    }
}
