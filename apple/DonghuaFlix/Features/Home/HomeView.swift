import SwiftUI

struct HomeView: View {
    @Environment(ShowRepository.self) private var showRepository
    @Environment(WatchRepository.self) private var watchRepository
    @Environment(Router.self) private var router

    @State private var viewModel: HomeViewModel?

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
