import Foundation
import Observation

@Observable
final class ShowRepository {

    private let apiClient: APIClient

    init(apiClient: APIClient) {
        self.apiClient = apiClient
    }

    // MARK: - Shows

    func getShows(page: Int = 1, pageSize: Int = 20, genre: String? = nil, status: String? = nil, category: String? = nil, website: String? = nil) async throws -> ShowListDTO {
        try await apiClient.request(.shows(page: page, pageSize: pageSize, genre: genre, status: status, category: category, website: website))
    }

    func getShow(id: Int) async throws -> Show? {
        let dto: ShowDTO = try await apiClient.request(.show(id: id))
        return dto.toDomain()
    }

    func searchShows(query: String, page: Int = 1, pageSize: Int = 20) async throws -> [Show] {
        let dto: ShowListDTO = try await apiClient.request(.searchShows(query: query, page: page, pageSize: pageSize))
        return dto.items.map { $0.toDomain() }
    }

    // MARK: - Episodes & Sources

    func getEpisodes(showId: Int, website: String? = nil) async throws -> [Episode] {
        let dtos: [EpisodeDTO] = try await apiClient.request(.episodes(showId: showId, website: website))
        return dtos.map { $0.toDomain() }
    }

    func getEpisodeSources(episodeId: Int) async throws -> [VideoSource] {
        let dtos: [SourceDTO] = try await apiClient.request(.episodeSources(episodeId: episodeId))
        return dtos.map { $0.toDomain() }
    }

    func resolveSource(sourceId: Int) async throws -> VideoSource? {
        let dto: SourceDTO = try await apiClient.request(.resolveSource(sourceId: sourceId))
        return dto.toDomain()
    }

    // MARK: - Discovery

    func getAllGenres() async throws -> [String] {
        try await apiClient.request(.genres)
    }

    func getHomeSections() async throws -> [HomeSection] {
        let response: HomeResponseDTO = try await apiClient.request(.home)
        return response.sections.map { $0.toDomain() }
    }

    func getShowWebsites(id: Int) async throws -> [WebsiteInfo] {
        let dtos: [WebsiteInfoDTO] = try await apiClient.request(.showWebsites(id: id))
        return dtos.map { $0.toDomain() }
    }
}
