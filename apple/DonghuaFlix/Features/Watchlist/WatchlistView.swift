import SwiftUI

struct WatchlistView: View {
    @Environment(WatchRepository.self) private var watchRepository
    @Environment(Router.self) private var router

    @State private var viewModel: WatchlistViewModel?

    private var columns: [GridItem] {
        #if os(macOS)
        return Array(repeating: GridItem(.adaptive(minimum: 170, maximum: 200), spacing: 16), count: 1)
        #else
        if UIDevice.current.userInterfaceIdiom == .pad {
            return Array(repeating: GridItem(.adaptive(minimum: 170, maximum: 200), spacing: 16), count: 1)
        } else {
            return Array(repeating: GridItem(.adaptive(minimum: 140, maximum: 170), spacing: 12), count: 1)
        }
        #endif
    }

    var body: some View {
        ZStack {
            DonghuaFlixTheme.backgroundGradient.ignoresSafeArea()

            if let vm = viewModel {
                if vm.isLoading && vm.shows.isEmpty {
                    loadingView
                } else if vm.shows.isEmpty {
                    emptyState
                } else {
                    gridContent(vm: vm)
                }
            } else {
                loadingView
            }
        }
        .navigationTitle("My List")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .task {
            if viewModel == nil {
                viewModel = WatchlistViewModel(watchRepository: watchRepository)
            }
            await viewModel?.loadWatchlist()
        }
        .refreshable {
            await viewModel?.loadWatchlist()
        }
    }

    // MARK: - Grid Content

    @ViewBuilder
    private func gridContent(vm: WatchlistViewModel) -> some View {
        ScrollView(.vertical, showsIndicators: false) {
            LazyVGrid(columns: columns, spacing: 16) {
                ForEach(vm.shows) { show in
                    ShowCardView(show: show)
                        .contextMenu {
                            Button(role: .destructive) {
                                Task { await vm.removeFromWatchlist(showId: show.id) }
                            } label: {
                                Label("Remove from My List", systemImage: "heart.slash")
                            }
                        }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 80)
        }
    }

    // MARK: - States

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(DonghuaFlixTheme.accentFuchsia)
                .scaleEffect(1.5)
            Text("Loading watchlist...")
                .foregroundStyle(DonghuaFlixTheme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "heart")
                .font(.system(size: 56))
                .foregroundStyle(DonghuaFlixTheme.accentPink.opacity(0.5))
            Text("Your List is Empty")
                .font(.title2.bold())
                .foregroundStyle(DonghuaFlixTheme.textPrimary)
            Text("Shows you add to your list will appear here")
                .font(.subheadline)
                .foregroundStyle(DonghuaFlixTheme.textSecondary)
                .multilineTextAlignment(.center)
            Button {
                router.navigate(to: .browse())
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "square.grid.2x2")
                        .font(.caption)
                    Text("Browse Shows")
                        .font(.subheadline.bold())
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(DonghuaFlixTheme.accentGradient)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
            .buttonStyle(.plain)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
