import Foundation
import Observation
import Combine

@Observable
final class SearchViewModel {

    var query = ""
    var results: [Show] = []
    var isSearching = false
    var hasSearched = false
    var error: String?

    private let showRepository: ShowRepository
    private var searchTask: Task<Void, Never>?

    init(showRepository: ShowRepository) {
        self.showRepository = showRepository
    }

    // MARK: - Search with Debounce

    @MainActor
    func onQueryChanged(_ newQuery: String) {
        query = newQuery
        searchTask?.cancel()

        guard !newQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            results = []
            hasSearched = false
            isSearching = false
            return
        }

        searchTask = Task {
            // 300ms debounce
            try? await Task.sleep(nanoseconds: 300_000_000)
            guard !Task.isCancelled else { return }
            await performSearch(query: newQuery)
        }
    }

    @MainActor
    private func performSearch(query: String) async {
        isSearching = true
        error = nil

        do {
            results = try await showRepository.searchShows(query: query)
            hasSearched = true
        } catch {
            if !Task.isCancelled {
                self.error = error.localizedDescription
            }
        }

        isSearching = false
    }
}
