import SwiftUI

struct BrowseView: View {
    let initialGenre: String?

    @Environment(ShowRepository.self) private var showRepository
    @Environment(Router.self) private var router

    @State private var viewModel: BrowseViewModel?

    init(genre: String? = nil) {
        self.initialGenre = genre
    }

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
                } else if let error = vm.error, vm.shows.isEmpty {
                    errorView(error)
                } else {
                    contentView(vm: vm)
                }
            } else {
                loadingView
            }
        }
        .navigationTitle(initialGenre ?? "Browse")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .task {
            if viewModel == nil {
                viewModel = BrowseViewModel(
                    showRepository: showRepository,
                    initialGenre: initialGenre
                )
            }
            await viewModel?.loadInitial()
        }
    }

    // MARK: - Content

    @ViewBuilder
    private func contentView(vm: BrowseViewModel) -> some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 16) {
                // Genre filter chips
                if !vm.genres.isEmpty {
                    genreFilter(vm: vm)
                }

                // Show grid
                LazyVGrid(columns: columns, spacing: 16) {
                    ForEach(vm.shows) { show in
                        ShowCardView(show: show)
                            .onAppear {
                                // Load more when near bottom
                                if show.id == vm.shows.last?.id {
                                    Task { await vm.loadMore() }
                                }
                            }
                    }
                }
                .padding(.horizontal, 16)

                if vm.isLoading && !vm.shows.isEmpty {
                    HStack {
                        Spacer()
                        ProgressView()
                            .tint(DonghuaFlixTheme.accentFuchsia)
                        Spacer()
                    }
                    .padding()
                }

                Spacer(minLength: 80)
            }
        }
    }

    // MARK: - Genre Filter

    @ViewBuilder
    private func genreFilter(vm: BrowseViewModel) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // "All" chip
                filterChip(title: "All", isSelected: vm.selectedGenre == nil) {
                    Task { await vm.selectGenre(nil) }
                }

                ForEach(vm.genres, id: \.self) { genre in
                    filterChip(title: genre, isSelected: vm.selectedGenre == genre) {
                        Task { await vm.selectGenre(genre) }
                    }
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
    }

    @ViewBuilder
    private func filterChip(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.caption.bold())
                .foregroundStyle(isSelected ? .white : DonghuaFlixTheme.textSecondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    isSelected
                        ? AnyShapeStyle(DonghuaFlixTheme.accentGradient)
                        : AnyShapeStyle(DonghuaFlixTheme.surfaceCard)
                )
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: - States

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(DonghuaFlixTheme.accentFuchsia)
                .scaleEffect(1.5)
            Text("Loading shows...")
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
                Task { await viewModel?.loadInitial() }
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
