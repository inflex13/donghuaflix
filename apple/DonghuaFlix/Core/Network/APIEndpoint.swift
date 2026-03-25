import Foundation

// MARK: - HTTP Method

enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case delete = "DELETE"
}

// MARK: - API Endpoint

enum APIEndpoint {

    // Shows
    case shows(page: Int = 1, pageSize: Int = 20, genre: String? = nil, status: String? = nil, category: String? = nil, website: String? = nil)
    case show(id: Int)
    case showWebsites(id: Int)
    case searchShows(query: String, page: Int = 1, pageSize: Int = 20)

    // Episodes & Sources
    case episodes(showId: Int, website: String? = nil)
    case episodeSources(episodeId: Int)
    case resolveSource(sourceId: Int)

    // Watch Tracking
    case updateProgress(WatchProgressRequest)
    case continueWatching
    case watchHistory(limit: Int = 50)

    // Watchlist
    case watchlist
    case addToWatchlist(showId: Int)
    case removeFromWatchlist(showId: Int)

    // Sync
    case sync(since: String? = nil)

    // Discovery
    case home
    case genres
    case websites

    // App
    case appVersion
    case crashLog(CrashLogRequest)

    // MARK: - Path

    var path: String {
        switch self {
        case .shows:
            return "/api/shows"
        case .show(let id):
            return "/api/shows/\(id)"
        case .showWebsites(let id):
            return "/api/shows/\(id)/websites"
        case .searchShows:
            return "/api/shows/search"
        case .episodes(let showId, _):
            return "/api/shows/\(showId)/episodes"
        case .episodeSources(let episodeId):
            return "/api/episodes/\(episodeId)/sources"
        case .resolveSource(let sourceId):
            return "/api/sources/\(sourceId)/resolve"
        case .updateProgress:
            return "/api/watch/progress"
        case .continueWatching:
            return "/api/watch/continue"
        case .watchHistory:
            return "/api/watch/history"
        case .watchlist:
            return "/api/watchlist"
        case .addToWatchlist(let showId):
            return "/api/watchlist/\(showId)"
        case .removeFromWatchlist(let showId):
            return "/api/watchlist/\(showId)"
        case .sync:
            return "/api/sync"
        case .home:
            return "/api/home"
        case .genres:
            return "/api/genres"
        case .websites:
            return "/api/websites"
        case .appVersion:
            return "/app/version"
        case .crashLog:
            return "/api/crash-logs"
        }
    }

    // MARK: - HTTP Method

    var method: HTTPMethod {
        switch self {
        case .shows, .show, .showWebsites, .searchShows,
             .episodes, .episodeSources,
             .continueWatching, .watchHistory,
             .watchlist,
             .sync, .home, .genres, .websites, .appVersion:
            return .get
        case .resolveSource, .updateProgress, .addToWatchlist, .crashLog:
            return .post
        case .removeFromWatchlist:
            return .delete
        }
    }

    // MARK: - Query Items

    var queryItems: [URLQueryItem]? {
        switch self {
        case .shows(let page, let pageSize, let genre, let status, let category, let website):
            var items = [
                URLQueryItem(name: "page", value: "\(page)"),
                URLQueryItem(name: "page_size", value: "\(pageSize)"),
            ]
            if let genre { items.append(URLQueryItem(name: "genre", value: genre)) }
            if let status { items.append(URLQueryItem(name: "status", value: status)) }
            if let category { items.append(URLQueryItem(name: "category", value: category)) }
            if let website { items.append(URLQueryItem(name: "website", value: website)) }
            return items

        case .searchShows(let query, let page, let pageSize):
            return [
                URLQueryItem(name: "q", value: query),
                URLQueryItem(name: "page", value: "\(page)"),
                URLQueryItem(name: "page_size", value: "\(pageSize)"),
            ]

        case .episodes(_, let website):
            if let website {
                return [URLQueryItem(name: "website", value: website)]
            }
            return nil

        case .watchHistory(let limit):
            return [URLQueryItem(name: "limit", value: "\(limit)")]

        case .sync(let since):
            if let since {
                return [URLQueryItem(name: "since", value: since)]
            }
            return nil

        default:
            return nil
        }
    }

    // MARK: - Body

    var body: Data? {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase

        switch self {
        case .updateProgress(let request):
            return try? encoder.encode(request)
        case .crashLog(let request):
            return try? encoder.encode(request)
        default:
            return nil
        }
    }

    // MARK: - Build URLRequest

    func urlRequest() throws -> URLRequest {
        guard var components = URLComponents(string: APIClient.baseURL + path) else {
            throw APIError.invalidURL
        }

        if let queryItems, !queryItems.isEmpty {
            components.queryItems = queryItems
        }

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = body

        return request
    }
}
