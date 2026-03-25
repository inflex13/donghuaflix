import SwiftUI

struct DetailView: View {
    let showId: Int
    let resumeEpisode: Int?

    @Environment(ShowRepository.self) private var showRepository
    @Environment(WatchRepository.self) private var watchRepository
    @Environment(Router.self) private var router
    @Environment(\.horizontalSizeClass) private var sizeClass

    @State private var viewModel: DetailViewModel?

    var body: some View {
        ZStack {
            DonghuaFlixTheme.backgroundGradient.ignoresSafeArea()

            if let vm = viewModel {
                if vm.isLoading && vm.show == nil {
                    loadingView
                } else if let error = vm.error, vm.show == nil {
                    errorView(error)
                } else if let show = vm.show {
                    contentView(show: show, vm: vm)
                }
            } else {
                loadingView
            }
        }
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .task {
            if viewModel == nil {
                viewModel = DetailViewModel(
                    showRepository: showRepository,
                    watchRepository: watchRepository
                )
            }
            await viewModel?.loadShow(id: showId)
        }
    }

    // MARK: - Content Layout

    @ViewBuilder
    private func contentView(show: Show, vm: DetailViewModel) -> some View {
        let isCompact = sizeClass == .compact
        if isCompact {
            compactLayout(show: show, vm: vm)
        } else {
            regularLayout(show: show, vm: vm)
        }
    }

    // MARK: - iPhone Layout (Compact)

    @ViewBuilder
    private func compactLayout(show: Show, vm: DetailViewModel) -> some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 16) {
                // Poster
                posterView(show: show, height: 250)
                    .frame(maxWidth: .infinity)

                // Info
                showInfoSection(show: show, vm: vm)
                    .padding(.horizontal, 16)

                // Action buttons
                actionButtons(vm: vm)
                    .padding(.horizontal, 16)

                // Website selector
                if show.websites.count > 1 {
                    websiteSelector(show: show, vm: vm)
                        .padding(.horizontal, 16)
                }

                // Episodes
                episodesSection(vm: vm)

                Spacer(minLength: 80)
            }
        }
    }

    // MARK: - iPad/Mac Layout (Regular)

    @ViewBuilder
    private func regularLayout(show: Show, vm: DetailViewModel) -> some View {
        HStack(alignment: .top, spacing: 20) {
            // Left: poster
            VStack(spacing: 16) {
                posterView(show: show, height: 330)
                    .frame(width: 220)

                actionButtons(vm: vm)
                    .frame(width: 220)
            }
            .padding(.leading, 16)
            .padding(.top, 16)

            // Right: info + episodes
            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    showInfoSection(show: show, vm: vm)

                    if show.websites.count > 1 {
                        websiteSelector(show: show, vm: vm)
                    }

                    episodesSection(vm: vm)

                    Spacer(minLength: 40)
                }
                .padding(.trailing, 16)
                .padding(.top, 16)
            }
        }
    }

    // MARK: - Poster

    @ViewBuilder
    private func posterView(show: Show, height: CGFloat) -> some View {
        if let urlString = show.posterUrl, let url = URL(string: urlString) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(height: height)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                default:
                    RoundedRectangle(cornerRadius: 12)
                        .fill(DonghuaFlixTheme.surfaceCard)
                        .frame(height: height)
                        .overlay {
                            Image(systemName: "film")
                                .font(.system(size: 40))
                                .foregroundStyle(DonghuaFlixTheme.textMuted)
                        }
                }
            }
        } else {
            RoundedRectangle(cornerRadius: 12)
                .fill(DonghuaFlixTheme.surfaceCard)
                .frame(height: height)
                .overlay {
                    Image(systemName: "film")
                        .font(.system(size: 40))
                        .foregroundStyle(DonghuaFlixTheme.textMuted)
                }
        }
    }

    // MARK: - Show Info

    @ViewBuilder
    private func showInfoSection(show: Show, vm: DetailViewModel) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(show.title)
                .font(.title2.bold())
                .foregroundStyle(DonghuaFlixTheme.textPrimary)

            if let titleChinese = show.titleChinese, !titleChinese.isEmpty {
                Text(titleChinese)
                    .font(.subheadline)
                    .foregroundStyle(DonghuaFlixTheme.textSecondary)
            }

            // Metadata row
            HStack(spacing: 10) {
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
                if let status = show.status {
                    Text(status)
                        .font(.caption.bold())
                        .foregroundStyle(DonghuaFlixTheme.accentPurple)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(DonghuaFlixTheme.accentPurple.opacity(0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                }
            }

            // Genres
            if !show.genres.isEmpty {
                FlowLayout(spacing: 6) {
                    ForEach(show.genres, id: \.self) { genre in
                        Text(genre)
                            .font(.caption)
                            .foregroundStyle(DonghuaFlixTheme.textSecondary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(DonghuaFlixTheme.surfaceCard)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                }
            }

            // Description
            if let description = show.description, !description.isEmpty {
                Text(description)
                    .font(.subheadline)
                    .foregroundStyle(DonghuaFlixTheme.textSecondary)
                    .lineLimit(5)
                    .padding(.top, 4)
            }
        }
    }

    // MARK: - Action Buttons

    @ViewBuilder
    private func actionButtons(vm: DetailViewModel) -> some View {
        VStack(spacing: 10) {
            // Play / Resume button
            Button {
                if let epNum = vm.nextEpisodeNumber {
                    router.push(.player(
                        showId: showId,
                        episodeNumber: epNum,
                        website: vm.selectedWebsite?.name
                    ))
                }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "play.fill")
                        .font(.subheadline)
                    if let lastWatched = vm.lastWatched {
                        if lastWatched.completed {
                            Text("Resume EP \(lastWatched.episodeNumber + 1)")
                                .font(.subheadline.bold())
                        } else {
                            Text("Resume EP \(lastWatched.episodeNumber)")
                                .font(.subheadline.bold())
                        }
                    } else {
                        Text("Play")
                            .font(.subheadline.bold())
                    }
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(DonghuaFlixTheme.accentGradient)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
            .buttonStyle(.plain)

            // My List button
            Button {
                Task { await vm.toggleWatchlist() }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: vm.isInWatchlist ? "heart.fill" : "heart")
                        .font(.subheadline)
                        .foregroundStyle(vm.isInWatchlist ? DonghuaFlixTheme.accentPink : DonghuaFlixTheme.textSecondary)
                    Text(vm.isInWatchlist ? "In My List" : "Add to My List")
                        .font(.subheadline.bold())
                        .foregroundStyle(DonghuaFlixTheme.textPrimary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(DonghuaFlixTheme.surfaceCard)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Website Selector

    @ViewBuilder
    private func websiteSelector(show: Show, vm: DetailViewModel) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Source")
                .font(.caption.bold())
                .foregroundStyle(DonghuaFlixTheme.textMuted)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(show.websites) { website in
                        Button {
                            Task { await vm.selectWebsite(website) }
                        } label: {
                            HStack(spacing: 4) {
                                Text(website.displayName)
                                    .font(.caption.bold())
                                if let count = website.episodeCount {
                                    Text("(\(count))")
                                        .font(.caption2)
                                }
                            }
                            .foregroundStyle(
                                vm.selectedWebsite?.id == website.id
                                    ? Color.white
                                    : DonghuaFlixTheme.textSecondary
                            )
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(
                                vm.selectedWebsite?.id == website.id
                                    ? AnyShapeStyle(DonghuaFlixTheme.accentGradient)
                                    : AnyShapeStyle(DonghuaFlixTheme.surfaceCard)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    // MARK: - Episodes Section

    @ViewBuilder
    private func episodesSection(vm: DetailViewModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Text("Episodes")
                    .font(.title3.bold())
                    .foregroundStyle(DonghuaFlixTheme.textPrimary)

                Text("(\(vm.episodes.count))")
                    .font(.subheadline)
                    .foregroundStyle(DonghuaFlixTheme.textMuted)

                Spacer()

                // Sort toggle
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        vm.latestFirst.toggle()
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: vm.latestFirst ? "arrow.down" : "arrow.up")
                            .font(.caption)
                        Text(vm.latestFirst ? "Latest" : "Oldest")
                            .font(.caption.bold())
                    }
                    .foregroundStyle(DonghuaFlixTheme.accentPurple)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(DonghuaFlixTheme.accentPurple.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                }
                .buttonStyle(.plain)

                // Mark all watched
                Menu {
                    if let lastEp = vm.episodes.max(by: { $0.episodeNumber < $1.episodeNumber })?.episodeNumber {
                        Button("Mark All as Watched") {
                            Task { await vm.markAllWatchedUpTo(lastEp) }
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.subheadline)
                        .foregroundStyle(DonghuaFlixTheme.textSecondary)
                }
            }
            .padding(.horizontal, 16)

            // Page selector for 50+ episodes
            if vm.totalPages > 1 {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(0..<vm.totalPages, id: \.self) { page in
                            Button {
                                withAnimation { vm.currentPage = page }
                            } label: {
                                Text("\(page + 1)")
                                    .font(.caption.bold())
                                    .foregroundStyle(
                                        vm.currentPage == page ? .white : DonghuaFlixTheme.textSecondary
                                    )
                                    .frame(width: 32, height: 32)
                                    .background(
                                        vm.currentPage == page
                                            ? AnyShapeStyle(DonghuaFlixTheme.accentGradient)
                                            : AnyShapeStyle(DonghuaFlixTheme.surfaceCard)
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: 6))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                }
            }

            // Episode list
            LazyVStack(spacing: 2) {
                ForEach(vm.pagedEpisodes) { episode in
                    episodeRow(episode: episode, vm: vm)
                }
            }
        }
    }

    // MARK: - Episode Row

    @ViewBuilder
    private func episodeRow(episode: Episode, vm: DetailViewModel) -> some View {
        let progress = vm.watchedEpisodes[episode.episodeNumber]
        let isWatched = progress?.completed == true

        Button {
            router.push(.player(
                showId: showId,
                episodeNumber: episode.episodeNumber,
                website: vm.selectedWebsite?.name
            ))
        } label: {
            HStack(spacing: 12) {
                // Episode number badge
                Text("\(episode.episodeNumber)")
                    .font(.caption.bold())
                    .foregroundStyle(isWatched ? DonghuaFlixTheme.accentPurple : DonghuaFlixTheme.textPrimary)
                    .frame(width: 36, height: 36)
                    .background(
                        isWatched
                            ? DonghuaFlixTheme.accentPurple.opacity(0.15)
                            : DonghuaFlixTheme.surfaceCard
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                // Episode info
                VStack(alignment: .leading, spacing: 3) {
                    Text(episode.title ?? "Episode \(episode.episodeNumber)")
                        .font(.subheadline)
                        .foregroundStyle(DonghuaFlixTheme.textPrimary)
                        .lineLimit(1)

                    HStack(spacing: 6) {
                        if let date = episode.createdAt {
                            Text(date.prefix(10))
                                .font(.caption2)
                                .foregroundStyle(DonghuaFlixTheme.textMuted)
                        }
                    }

                    // Progress bar
                    if let progress = progress, !progress.completed && progress.progressFraction > 0 {
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule()
                                    .fill(DonghuaFlixTheme.surfaceCard)
                                    .frame(height: 3)
                                Capsule()
                                    .fill(DonghuaFlixTheme.accentFuchsia)
                                    .frame(width: geo.size.width * progress.progressFraction, height: 3)
                            }
                        }
                        .frame(height: 3)
                    }
                }

                Spacer()

                // Preload button (if no sources cached)
                if !episode.hasSources {
                    if vm.preloadingEpisodes.contains(episode.episodeNumber) {
                        ProgressView()
                            .scaleEffect(0.7)
                            .tint(DonghuaFlixTheme.accentFuchsia)
                    } else if vm.preloadedEpisodes.contains(episode.episodeNumber) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.subheadline)
                            .foregroundStyle(DonghuaFlixTheme.accentPurple)
                    } else {
                        Button {
                            Task { await vm.preloadSources(for: episode) }
                        } label: {
                            Image(systemName: "arrow.down.circle")
                                .font(.subheadline)
                                .foregroundStyle(DonghuaFlixTheme.textMuted)
                        }
                        .buttonStyle(.plain)
                    }
                }

                // Watched toggle
                Button {
                    Task {
                        if isWatched {
                            await vm.markEpisodeUnwatched(episode)
                        } else {
                            await vm.markEpisodeWatched(episode)
                        }
                    }
                } label: {
                    Image(systemName: isWatched ? "checkmark.circle.fill" : "circle")
                        .font(.title3)
                        .foregroundStyle(
                            isWatched ? DonghuaFlixTheme.accentPurple : DonghuaFlixTheme.textMuted
                        )
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(DonghuaFlixTheme.obsidian)
        }
        .buttonStyle(.plain)
    }

    // MARK: - States

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(DonghuaFlixTheme.accentFuchsia)
                .scaleEffect(1.5)
            Text("Loading show...")
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
                Task { await viewModel?.loadShow(id: showId) }
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

// MARK: - Flow Layout (for genre chips)

struct FlowLayout: Layout {
    let spacing: CGFloat

    init(spacing: CGFloat = 8) {
        self.spacing = spacing
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = layout(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = layout(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y), proposal: .unspecified)
        }
    }

    private func layout(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var lineHeight: CGFloat = 0
        var totalHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if currentX + size.width > maxWidth && currentX > 0 {
                currentX = 0
                currentY += lineHeight + spacing
                lineHeight = 0
            }

            positions.append(CGPoint(x: currentX, y: currentY))
            lineHeight = max(lineHeight, size.height)
            totalHeight = currentY + lineHeight
            currentX += size.width + spacing
        }

        return (CGSize(width: maxWidth, height: totalHeight), positions)
    }
}
