import SwiftUI

struct ContentView: View {
    @Environment(Router.self) private var router
    @Environment(APIClient.self) private var apiClient
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        Group {
            if sizeClass == .compact {
                compactLayout
            } else {
                regularLayout
            }
        }
        .tint(DonghuaFlixTheme.accentFuchsia)
    }

    // MARK: - Compact Layout (iPhone)

    @ViewBuilder
    private var compactLayout: some View {
        @Bindable var router = router

        TabView(selection: $router.selectedTab) {
            ForEach(AppTab.allCases) { tab in
                NavigationStack(path: router.pathBinding(for: tab)) {
                    tabContent(for: tab)
                        .navigationDestination(for: AppRoute.self) { route in
                            destinationView(for: route)
                        }
                }
                .tabItem {
                    Label(tab.rawValue, systemImage: tab.systemImage)
                }
                .tag(tab)
            }
        }
        .donghuaFlixBackground()
    }

    // MARK: - Regular Layout (iPad / Mac)

    @ViewBuilder
    private var regularLayout: some View {
        @Bindable var router = router

        NavigationSplitView {
            List(AppTab.allCases, selection: $router.sidebarSelection) { tab in
                Label(tab.rawValue, systemImage: tab.systemImage)
                    .tag(tab)
            }
            .listStyle(.sidebar)
            .navigationTitle("DonghuaFlix")
        } detail: {
            if let tab = router.sidebarSelection {
                NavigationStack(path: router.pathBinding(for: tab)) {
                    tabContent(for: tab)
                        .navigationDestination(for: AppRoute.self) { route in
                            destinationView(for: route)
                        }
                }
            } else {
                placeholderView
            }
        }
        .navigationSplitViewStyle(.balanced)
    }

    // MARK: - Tab Content

    @ViewBuilder
    private func tabContent(for tab: AppTab) -> some View {
        switch tab {
        case .home:
            HomeView()
        case .browse:
            BrowseView()
        case .search:
            SearchView()
        case .myList:
            WatchlistView()
        }
    }

    // MARK: - Route Destinations

    @ViewBuilder
    private func destinationView(for route: AppRoute) -> some View {
        switch route {
        case .home:
            HomeView()
        case .detail(let showId, let resumeEpisode):
            DetailView(showId: showId, resumeEpisode: resumeEpisode)
        case .player(let showId, let episodeNumber, let website):
            PlayerView(showId: showId, episodeNumber: episodeNumber, website: website)
        case .browse(let genre):
            BrowseView(genre: genre)
        case .search:
            SearchView()
        case .watchlist:
            WatchlistView()
        }
    }

    // MARK: - Placeholder

    private var placeholderView: some View {
        VStack(spacing: 16) {
            Image(systemName: "tv")
                .font(.system(size: 64))
                .foregroundStyle(DonghuaFlixTheme.accentFuchsia)
            Text("DonghuaFlix")
                .font(.largeTitle.bold())
                .foregroundStyle(DonghuaFlixTheme.textPrimary)
            Text("Select a section from the sidebar")
                .foregroundStyle(DonghuaFlixTheme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .donghuaFlixBackground()
    }
}

#Preview {
    let apiClient = APIClient()
    ContentView()
        .environment(Router())
        .environment(apiClient)
        .environment(ShowRepository(apiClient: apiClient))
        .environment(WatchRepository(apiClient: apiClient))
}
