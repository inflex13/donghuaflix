import SwiftUI
import Observation

// MARK: - App Route

enum AppRoute: Hashable {
    case home
    case detail(showId: Int, resumeEpisode: Int? = nil)
    case player(showId: Int, episodeNumber: Int, website: String?)
    case browse(genre: String? = nil)
    case search
    case watchlist
}

// MARK: - Tab

enum AppTab: String, CaseIterable, Identifiable {
    case home = "Home"
    case browse = "Browse"
    case search = "Search"
    case myList = "My List"

    var id: String { rawValue }

    var systemImage: String {
        switch self {
        case .home: return "house.fill"
        case .browse: return "square.grid.2x2.fill"
        case .search: return "magnifyingglass"
        case .myList: return "heart.fill"
        }
    }
}

// MARK: - Router

@Observable
final class Router {

    var selectedTab: AppTab = .home

    // Separate navigation path per tab for iPhone
    var homePath = NavigationPath()
    var browsePath = NavigationPath()
    var searchPath = NavigationPath()
    var myListPath = NavigationPath()

    // Sidebar selection for iPad/Mac
    var sidebarSelection: AppTab? = .home

    // MARK: - Navigation

    func navigate(to route: AppRoute) {
        switch route {
        case .home:
            selectedTab = .home
            sidebarSelection = .home

        case .detail, .player:
            // Push onto the current tab's navigation stack
            currentPath.append(route)

        case .browse:
            selectedTab = .browse
            sidebarSelection = .browse
            if case .browse(let genre) = route, genre != nil {
                browsePath.append(route)
            }

        case .search:
            selectedTab = .search
            sidebarSelection = .search

        case .watchlist:
            selectedTab = .myList
            sidebarSelection = .myList
        }
    }

    func push(_ route: AppRoute) {
        currentPath.append(route)
    }

    func pop() {
        guard !currentPath.isEmpty else { return }
        currentPath.removeLast()
    }

    func popToRoot() {
        currentPath = NavigationPath()
    }

    // MARK: - Current Path (binds to the active tab)

    private var currentPath: NavigationPath {
        get {
            switch selectedTab {
            case .home: return homePath
            case .browse: return browsePath
            case .search: return searchPath
            case .myList: return myListPath
            }
        }
        set {
            switch selectedTab {
            case .home: homePath = newValue
            case .browse: browsePath = newValue
            case .search: searchPath = newValue
            case .myList: myListPath = newValue
            }
        }
    }

    // MARK: - Binding helpers for NavigationStack

    func pathBinding(for tab: AppTab) -> Binding<NavigationPath> {
        switch tab {
        case .home:
            return Binding(get: { self.homePath }, set: { self.homePath = $0 })
        case .browse:
            return Binding(get: { self.browsePath }, set: { self.browsePath = $0 })
        case .search:
            return Binding(get: { self.searchPath }, set: { self.searchPath = $0 })
        case .myList:
            return Binding(get: { self.myListPath }, set: { self.myListPath = $0 })
        }
    }
}
