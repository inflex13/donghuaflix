import Foundation
import Observation

@Observable
final class BrowseViewModel {

    var shows: [Show] = []
    var genres: [String] = []
    var selectedGenre: String?
    var selectedWebsite: String?
    var isLoading = false
    var error: String?
    var currentPage = 1
    var totalShows = 0
    var hasMore: Bool {
        shows.count < totalShows
    }

    private let showRepository: ShowRepository
    private let pageSize = 30

    init(showRepository: ShowRepository, initialGenre: String? = nil) {
        self.showRepository = showRepository
        self.selectedGenre = initialGenre
    }

    // MARK: - Load

    @MainActor
    func loadInitial() async {
        isLoading = true
        error = nil
        currentPage = 1
        shows = []

        do {
            // Load genres for filter
            if genres.isEmpty {
                genres = try await showRepository.getAllGenres()
            }

            // Load shows
            let result = try await showRepository.getShows(
                page: 1,
                pageSize: pageSize,
                genre: selectedGenre,
                website: selectedWebsite
            )
            shows = result.items.map { $0.toDomain() }
            totalShows = result.total
            currentPage = 1
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    @MainActor
    func loadMore() async {
        guard !isLoading && hasMore else { return }

        let nextPage = currentPage + 1

        do {
            let result = try await showRepository.getShows(
                page: nextPage,
                pageSize: pageSize,
                genre: selectedGenre,
                website: selectedWebsite
            )
            let newShows = result.items.map { $0.toDomain() }
            shows.append(contentsOf: newShows)
            currentPage = nextPage
            totalShows = result.total
        } catch {
            // Silently fail pagination
        }
    }

    // MARK: - Filters

    @MainActor
    func selectGenre(_ genre: String?) async {
        guard genre != selectedGenre else {
            selectedGenre = nil
            await loadInitial()
            return
        }
        selectedGenre = genre
        await loadInitial()
    }

    @MainActor
    func selectWebsite(_ website: String?) async {
        guard website != selectedWebsite else {
            selectedWebsite = nil
            await loadInitial()
            return
        }
        selectedWebsite = website
        await loadInitial()
    }
}
