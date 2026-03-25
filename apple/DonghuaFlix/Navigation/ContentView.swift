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
            HomeTabPlaceholder()
        case .browse:
            BrowseTabPlaceholder()
        case .search:
            SearchTabPlaceholder()
        case .myList:
            MyListTabPlaceholder()
        }
    }

    // MARK: - Route Destinations

    @ViewBuilder
    private func destinationView(for route: AppRoute) -> some View {
        switch route {
        case .home:
            HomeTabPlaceholder()
        case .detail(let showId, let resumeEpisode):
            DetailPlaceholder(showId: showId, resumeEpisode: resumeEpisode)
        case .player(let showId, let episodeNumber, let website):
            PlayerPlaceholder(showId: showId, episodeNumber: episodeNumber, website: website)
        case .browse(let genre):
            BrowseTabPlaceholder(genre: genre)
        case .search:
            SearchTabPlaceholder()
        case .watchlist:
            MyListTabPlaceholder()
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

// MARK: - Placeholder Views (to be replaced by actual feature views)

struct HomeTabPlaceholder: View {
    var body: some View {
        ZStack {
            DonghuaFlixTheme.backgroundGradient.ignoresSafeArea()
            VStack(spacing: 12) {
                Image(systemName: "house.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(DonghuaFlixTheme.accentPurple)
                Text("Home")
                    .font(.title.bold())
                    .foregroundStyle(DonghuaFlixTheme.textPrimary)
                Text("Coming soon")
                    .foregroundStyle(DonghuaFlixTheme.textSecondary)
            }
        }
        .navigationTitle("Home")
    }
}

struct BrowseTabPlaceholder: View {
    var genre: String? = nil

    var body: some View {
        ZStack {
            DonghuaFlixTheme.backgroundGradient.ignoresSafeArea()
            VStack(spacing: 12) {
                Image(systemName: "square.grid.2x2.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(DonghuaFlixTheme.accentFuchsia)
                Text(genre ?? "Browse")
                    .font(.title.bold())
                    .foregroundStyle(DonghuaFlixTheme.textPrimary)
                Text("Coming soon")
                    .foregroundStyle(DonghuaFlixTheme.textSecondary)
            }
        }
        .navigationTitle(genre ?? "Browse")
    }
}

struct SearchTabPlaceholder: View {
    var body: some View {
        ZStack {
            DonghuaFlixTheme.backgroundGradient.ignoresSafeArea()
            VStack(spacing: 12) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 48))
                    .foregroundStyle(DonghuaFlixTheme.accentGold)
                Text("Search")
                    .font(.title.bold())
                    .foregroundStyle(DonghuaFlixTheme.textPrimary)
                Text("Coming soon")
                    .foregroundStyle(DonghuaFlixTheme.textSecondary)
            }
        }
        .navigationTitle("Search")
    }
}

struct MyListTabPlaceholder: View {
    var body: some View {
        ZStack {
            DonghuaFlixTheme.backgroundGradient.ignoresSafeArea()
            VStack(spacing: 12) {
                Image(systemName: "heart.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(DonghuaFlixTheme.accentPink)
                Text("My List")
                    .font(.title.bold())
                    .foregroundStyle(DonghuaFlixTheme.textPrimary)
                Text("Coming soon")
                    .foregroundStyle(DonghuaFlixTheme.textSecondary)
            }
        }
        .navigationTitle("My List")
    }
}

struct DetailPlaceholder: View {
    let showId: Int
    let resumeEpisode: Int?

    var body: some View {
        ZStack {
            DonghuaFlixTheme.backgroundGradient.ignoresSafeArea()
            VStack(spacing: 12) {
                Text("Show #\(showId)")
                    .font(.title.bold())
                    .foregroundStyle(DonghuaFlixTheme.textPrimary)
                if let ep = resumeEpisode {
                    Text("Resume from Episode \(ep)")
                        .foregroundStyle(DonghuaFlixTheme.textSecondary)
                }
            }
        }
        .navigationTitle("Detail")
    }
}

struct PlayerPlaceholder: View {
    let showId: Int
    let episodeNumber: Int
    let website: String?

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 12) {
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(DonghuaFlixTheme.accentFuchsia)
                Text("Player - Show #\(showId) Ep \(episodeNumber)")
                    .font(.title2.bold())
                    .foregroundStyle(DonghuaFlixTheme.textPrimary)
                if let website {
                    Text("Source: \(website)")
                        .foregroundStyle(DonghuaFlixTheme.textSecondary)
                }
            }
        }
        #if os(iOS)
        .navigationBarHidden(true)
        #endif
    }
}

#Preview {
    ContentView()
        .environment(Router())
        .environment(APIClient())
}
