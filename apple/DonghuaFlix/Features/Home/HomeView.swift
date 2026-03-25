import SwiftUI

struct HomeView: View {
    @Environment(ShowRepository.self) private var showRepository
    @Environment(WatchRepository.self) private var watchRepository
    @Environment(Router.self) private var router

    @State private var viewModel: HomeViewModel?

    private var heroShow: Show? {
        viewModel?.sections.first?.shows.first
    }

    var body: some View {
        ZStack {
            DonghuaFlixTheme.backgroundGradient.ignoresSafeArea()

            if let vm = viewModel {
                if vm.isLoading && vm.sections.isEmpty {
                    loadingView
                } else if let error = vm.error, vm.sections.isEmpty {
                    errorView(error)
                } else {
                    contentScrollView(vm)
                }
            } else {
                loadingView
            }
        }
        .navigationTitle("DonghuaFlix")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .task {
            if viewModel == nil {
                viewModel = HomeViewModel(
                    showRepository: showRepository,
                    watchRepository: watchRepository
                )
            }
            await viewModel?.loadHome()
        }
        .refreshable {
            await viewModel?.fullResync()
        }
        .toolbar {
            ToolbarItem(placement: .automatic) {
                Button {
                    Task { await viewModel?.fullResync() }
                } label: {
                    Label("Sync", systemImage: "arrow.triangle.2.circlepath")
                }
                .tint(DonghuaFlixTheme.accentFuchsia)
                .disabled(viewModel?.isLoading == true)
            }
        }
    }

    // MARK: - Content

    @ViewBuilder
    private func contentScrollView(_ vm: HomeViewModel) -> some View {
        ScrollView(.vertical, showsIndicators: false) {
            LazyVStack(alignment: .leading, spacing: 24) {
                // Hero banner
                if let hero = heroShow {
                    heroBanner(hero)
                }

                // Continue watching
                if !vm.continueWatching.isEmpty {
                    sectionHeader(title: "Continue Watching", accentColor: DonghuaFlixTheme.accentFuchsia)
                    continueWatchingRow(vm.continueWatching)
                }

                // Section rows
                ForEach(vm.sections) { section in
                    sectionHeader(title: section.title, accentColor: DonghuaFlixTheme.accentPurple)
                    showRow(section.shows)
                }

                Spacer(minLength: 80)
            }
        }
    }

    // MARK: - Hero Banner

    @ViewBuilder
    private func heroBanner(_ show: Show) -> some View {
        Button {
            router.navigate(to: .detail(showId: show.id))
        } label: {
            ZStack(alignment: .bottomLeading) {
                // Background image
                if let urlString = show.posterUrl, let url = URL(string: urlString) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(maxWidth: .infinity)
                                .frame(height: heroBannerHeight)
                                .clipped()
                        default:
                            DonghuaFlixTheme.surfaceDark
                                .frame(height: heroBannerHeight)
                        }
                    }
                } else {
                    DonghuaFlixTheme.surfaceDark
                        .frame(height: heroBannerHeight)
                }

                // Gradient overlays
                VStack(spacing: 0) {
                    DonghuaFlixTheme.fadeToBlackTop
                        .frame(height: 80)
                    Spacer()
                    DonghuaFlixTheme.fadeToBlackBottom
                        .frame(height: 160)
                }
                .frame(height: heroBannerHeight)

                // Info overlay
                VStack(alignment: .leading, spacing: 8) {
                    Text(show.title)
                        .font(.title.bold())
                        .foregroundStyle(DonghuaFlixTheme.textPrimary)
                        .lineLimit(2)

                    HStack(spacing: 8) {
                        if let rating = show.rating, rating > 0 {
                            HStack(spacing: 3) {
                                Image(systemName: "star.fill")
                                    .font(.caption)
                                    .foregroundStyle(DonghuaFlixTheme.accentGold)
                                Text(String(format: "%.1f", rating))
                                    .font(.subheadline.bold())
                                    .foregroundStyle(DonghuaFlixTheme.textPrimary)
                            }
                        }
                        if let year = show.year {
                            Text("\(year)")
                                .font(.subheadline)
                                .foregroundStyle(DonghuaFlixTheme.textSecondary)
                        }
                    }

                    if !show.genres.isEmpty {
                        Text(show.genres.prefix(3).joined(separator: " · "))
                            .font(.caption)
                            .foregroundStyle(DonghuaFlixTheme.textSecondary)
                    }

                    // Watch Now button
                    HStack(spacing: 6) {
                        Image(systemName: "play.fill")
                            .font(.caption)
                        Text("Watch Now")
                            .font(.subheadline.bold())
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(DonghuaFlixTheme.accentGradient)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .padding(16)
            }
            .frame(height: heroBannerHeight)
            .clipShape(RoundedRectangle(cornerRadius: 0))
        }
        .buttonStyle(.plain)
    }

    private var heroBannerHeight: CGFloat {
        #if os(macOS)
        return 400
        #else
        return UIDevice.current.userInterfaceIdiom == .pad ? 400 : 300
        #endif
    }

    // MARK: - Section Header

    @ViewBuilder
    private func sectionHeader(title: String, accentColor: Color) -> some View {
        HStack(spacing: 8) {
            RoundedRectangle(cornerRadius: 2)
                .fill(accentColor)
                .frame(width: 4, height: 20)
            Text(title)
                .font(.title3.bold())
                .foregroundStyle(DonghuaFlixTheme.textPrimary)
        }
        .padding(.horizontal, 16)
    }

    // MARK: - Continue Watching Row

    @ViewBuilder
    private func continueWatchingRow(_ items: [(Show, WatchProgress)]) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 12) {
                ForEach(items, id: \.0.id) { show, progress in
                    ShowCardView(show: show, progress: progress)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Show Row

    @ViewBuilder
    private func showRow(_ shows: [Show]) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 12) {
                ForEach(shows) { show in
                    ShowCardView(show: show)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    // MARK: - States

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(DonghuaFlixTheme.accentFuchsia)
                .scaleEffect(1.5)
            Text("Loading...")
                .foregroundStyle(DonghuaFlixTheme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundStyle(DonghuaFlixTheme.accentGold)
            Text("Failed to Load")
                .font(.title2.bold())
                .foregroundStyle(DonghuaFlixTheme.textPrimary)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(DonghuaFlixTheme.textSecondary)
                .multilineTextAlignment(.center)
            Button("Retry") {
                Task { await viewModel?.loadHome() }
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 24)
            .padding(.vertical, 10)
            .background(DonghuaFlixTheme.accentGradient)
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
