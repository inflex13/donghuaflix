import SwiftUI

struct SearchView: View {
    @Environment(ShowRepository.self) private var showRepository
    @Environment(Router.self) private var router

    @State private var viewModel: SearchViewModel?
    @State private var searchText = ""

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

            VStack(spacing: 0) {
                // Search field
                searchField

                if let vm = viewModel {
                    if vm.isSearching {
                        Spacer()
                        ProgressView()
                            .tint(DonghuaFlixTheme.accentFuchsia)
                        Spacer()
                    } else if vm.hasSearched && vm.results.isEmpty {
                        Spacer()
                        emptyState
                        Spacer()
                    } else if !vm.results.isEmpty {
                        resultsGrid(vm: vm)
                    } else {
                        Spacer()
                        initialState
                        Spacer()
                    }
                } else {
                    Spacer()
                    initialState
                    Spacer()
                }
            }
        }
        .navigationTitle("Search")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .task {
            if viewModel == nil {
                viewModel = SearchViewModel(showRepository: showRepository)
            }
        }
    }

    // MARK: - Search Field

    @ViewBuilder
    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.subheadline)
                .foregroundStyle(DonghuaFlixTheme.textMuted)

            TextField("Search donghua...", text: $searchText)
                .font(.subheadline)
                .foregroundStyle(DonghuaFlixTheme.textPrimary)
                #if os(iOS)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                #endif
                .onChange(of: searchText) { _, newValue in
                    viewModel?.onQueryChanged(newValue)
                }

            if !searchText.isEmpty {
                Button {
                    searchText = ""
                    viewModel?.onQueryChanged("")
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.subheadline)
                        .foregroundStyle(DonghuaFlixTheme.textMuted)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(DonghuaFlixTheme.surfaceCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    // MARK: - Results Grid

    @ViewBuilder
    private func resultsGrid(vm: SearchViewModel) -> some View {
        ScrollView(.vertical, showsIndicators: false) {
            LazyVGrid(columns: columns, spacing: 16) {
                ForEach(vm.results) { show in
                    ShowCardView(show: show)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 80)
        }
    }

    // MARK: - Empty/Initial States

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundStyle(DonghuaFlixTheme.textMuted)
            Text("No results found")
                .font(.title3.bold())
                .foregroundStyle(DonghuaFlixTheme.textPrimary)
            Text("Try a different search term")
                .font(.subheadline)
                .foregroundStyle(DonghuaFlixTheme.textSecondary)
        }
    }

    private var initialState: some View {
        VStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundStyle(DonghuaFlixTheme.accentPurple.opacity(0.5))
            Text("Search Donghua")
                .font(.title3.bold())
                .foregroundStyle(DonghuaFlixTheme.textPrimary)
            Text("Find your next favorite show")
                .font(.subheadline)
                .foregroundStyle(DonghuaFlixTheme.textSecondary)
        }
    }
}
