import SwiftUI

struct ShowCardView: View {
    let show: Show
    let progress: WatchProgress?

    @Environment(Router.self) private var router

    init(show: Show, progress: WatchProgress? = nil) {
        self.show = show
        self.progress = progress
    }

    private var cardWidth: CGFloat {
        #if os(macOS)
        return 180
        #else
        return UIDevice.current.userInterfaceIdiom == .pad ? 180 : 150
        #endif
    }

    private var cardHeight: CGFloat {
        cardWidth * 1.5
    }

    var body: some View {
        Button {
            router.navigate(to: .detail(showId: show.id))
        } label: {
            ZStack(alignment: .topLeading) {
                // Poster image
                posterImage

                // Bottom gradient scrim with info
                VStack {
                    Spacer()
                    bottomInfo
                }

                // Rating badge (top-left)
                if let rating = show.rating, rating > 0 {
                    ratingBadge(rating)
                        .padding(8)
                }

                // Progress bar at bottom
                if let progress = progress, progress.progressFraction > 0 {
                    VStack {
                        Spacer()
                        progressBar(fraction: progress.progressFraction)
                    }
                }
            }
            .frame(width: cardWidth, height: cardHeight)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(ShowCardButtonStyle())
    }

    // MARK: - Subviews

    @ViewBuilder
    private var posterImage: some View {
        if let urlString = show.posterUrl, let url = URL(string: urlString) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: cardWidth, height: cardHeight)
                        .clipped()
                case .failure:
                    posterPlaceholder
                case .empty:
                    ZStack {
                        DonghuaFlixTheme.surfaceCard
                        ProgressView()
                            .tint(DonghuaFlixTheme.accentFuchsia)
                    }
                @unknown default:
                    posterPlaceholder
                }
            }
        } else {
            posterPlaceholder
        }
    }

    private var posterPlaceholder: some View {
        ZStack {
            DonghuaFlixTheme.surfaceDark
            VStack(spacing: 6) {
                Image(systemName: "film")
                    .font(.system(size: 28))
                    .foregroundStyle(DonghuaFlixTheme.textMuted)
                Text(show.title)
                    .font(.caption2)
                    .foregroundStyle(DonghuaFlixTheme.textMuted)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .padding(.horizontal, 8)
            }
        }
    }

    private var bottomInfo: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(show.title)
                .font(.caption.bold())
                .foregroundStyle(DonghuaFlixTheme.textPrimary)
                .lineLimit(2)

            HStack(spacing: 4) {
                if let year = show.year {
                    Text("\(year)")
                        .font(.caption2)
                        .foregroundStyle(DonghuaFlixTheme.textSecondary)
                }
                if let totalEps = show.totalEpisodes, totalEps > 0 {
                    if show.year != nil {
                        Text("·")
                            .font(.caption2)
                            .foregroundStyle(DonghuaFlixTheme.textMuted)
                    }
                    Text("\(totalEps) EP")
                        .font(.caption2)
                        .foregroundStyle(DonghuaFlixTheme.textSecondary)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(8)
        .background(
            LinearGradient(
                colors: [.clear, Color.black.opacity(0.7), Color.black.opacity(0.95)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private func ratingBadge(_ rating: Double) -> some View {
        HStack(spacing: 2) {
            Image(systemName: "star.fill")
                .font(.system(size: 8))
            Text(String(format: "%.1f", rating))
                .font(.caption2.bold())
        }
        .foregroundStyle(.black)
        .padding(.horizontal, 6)
        .padding(.vertical, 3)
        .background(DonghuaFlixTheme.accentGold)
        .clipShape(RoundedRectangle(cornerRadius: 4))
    }

    private func progressBar(fraction: Double) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(Color.black.opacity(0.5))
                    .frame(height: 3)
                Rectangle()
                    .fill(DonghuaFlixTheme.accentFuchsia)
                    .frame(width: geo.size.width * fraction, height: 3)
            }
        }
        .frame(height: 3)
    }
}

// MARK: - Button Style with Focus/Hover Scale

struct ShowCardButtonStyle: ButtonStyle {
    @State private var isHovering = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : isHovering ? 1.05 : 1.0)
            .animation(.easeInOut(duration: 0.15), value: configuration.isPressed)
            .animation(.easeInOut(duration: 0.2), value: isHovering)
            .onHover { hovering in
                isHovering = hovering
            }
            .shadow(
                color: isHovering ? DonghuaFlixTheme.accentFuchsia.opacity(0.3) : .clear,
                radius: isHovering ? 12 : 0
            )
    }
}
