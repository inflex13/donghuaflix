import Foundation
import Observation
import AVFoundation
import Combine

@Observable
final class PlayerViewModel {

    // MARK: - State

    var streamUrl: String?
    var streamType: StreamType = .unknown
    var subtitles: [SubtitleTrack] = []
    var sources: [VideoSource] = []
    var selectedSource: VideoSource?
    var episodes: [Episode] = []
    var episodeNumber: Int
    var showTitle: String = ""

    var isLoading = false
    var error: String?
    var resumePositionMs: Int = 0
    var showControls = true
    var autoPlayNext = true
    var hasNext = false
    var hasPrev = false

    var subtitleEnabled: Bool {
        didSet { UserDefaults.standard.set(subtitleEnabled, forKey: "subtitleEnabled") }
    }
    var subtitleSize: SubtitleSize {
        didSet { UserDefaults.standard.set(subtitleSize.rawValue, forKey: "subtitleSize") }
    }
    var subtitleBgEnabled: Bool {
        didSet { UserDefaults.standard.set(subtitleBgEnabled, forKey: "subtitleBgEnabled") }
    }

    // Internal
    private var showId: Int = 0
    private var website: String?
    private let showRepository: ShowRepository
    private let watchRepository: WatchRepository
    private let subtitleParser = SubtitleParser()
    private var controlsTimer: Task<Void, Never>?

    var currentSubtitleCue: SubtitleCue? {
        nil // Will be driven by the player view's time observer
    }

    init(showRepository: ShowRepository, watchRepository: WatchRepository) {
        self.showRepository = showRepository
        self.watchRepository = watchRepository
        self.episodeNumber = 1

        // Load persisted subtitle preferences
        self.subtitleEnabled = UserDefaults.standard.object(forKey: "subtitleEnabled") as? Bool ?? true
        let sizeRaw = UserDefaults.standard.string(forKey: "subtitleSize") ?? SubtitleSize.medium.rawValue
        self.subtitleSize = SubtitleSize(rawValue: sizeRaw) ?? .medium
        self.subtitleBgEnabled = UserDefaults.standard.object(forKey: "subtitleBgEnabled") as? Bool ?? true
    }

    // MARK: - Load

    @MainActor
    func loadPlayer(showId: Int, episodeNumber: Int, website: String?) async {
        self.showId = showId
        self.episodeNumber = episodeNumber
        self.website = website
        isLoading = true
        error = nil

        do {
            // Fetch show for title
            if let show = try await showRepository.getShow(id: showId) {
                showTitle = show.title
            }

            // Fetch episodes
            episodes = try await showRepository.getEpisodes(showId: showId, website: website)

            // Find current episode
            guard let episode = episodes.first(where: { $0.episodeNumber == episodeNumber }) else {
                error = "Episode \(episodeNumber) not found"
                isLoading = false
                return
            }

            // Update nav state
            updateNavState()

            // Fetch sources
            sources = try await showRepository.getEpisodeSources(episodeId: episode.id)

            guard let firstSource = sources.first else {
                error = "No sources available"
                isLoading = false
                return
            }

            // Resolve and select first source
            await selectSource(firstSource)

            // Get resume position from watch history
            let history = try await watchRepository.getWatchedEpisodesForShow(showId: showId)
            if let progress = history[episodeNumber], !progress.completed {
                resumePositionMs = progress.progressSeconds * 1000
            } else {
                resumePositionMs = 0
            }

        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    // MARK: - Source Selection

    @MainActor
    func selectSource(_ source: VideoSource) async {
        selectedSource = source

        // If source URL needs resolving
        if source.sourceUrl == nil || source.sourceUrl?.isEmpty == true {
            do {
                if let resolved = try await showRepository.resolveSource(sourceId: source.id) {
                    streamUrl = resolved.sourceUrl
                    streamType = resolved.streamType
                    subtitles = resolved.subtitles
                } else {
                    error = "Could not resolve source"
                }
            } catch {
                self.error = error.localizedDescription
            }
        } else {
            streamUrl = source.sourceUrl
            streamType = source.streamType
            subtitles = source.subtitles
        }

        // Load subtitles if available
        if let firstSub = subtitles.first, let url = URL(string: firstSub.url) {
            try? await subtitleParser.load(from: url)
        }
    }

    // MARK: - Episode Navigation

    @MainActor
    func nextEpisode() async {
        let next = episodeNumber + 1
        guard episodes.contains(where: { $0.episodeNumber == next }) else { return }
        await loadPlayer(showId: showId, episodeNumber: next, website: website)
    }

    @MainActor
    func previousEpisode() async {
        let prev = episodeNumber - 1
        guard prev >= 1, episodes.contains(where: { $0.episodeNumber == prev }) else { return }
        await loadPlayer(showId: showId, episodeNumber: prev, website: website)
    }

    @MainActor
    func onPlaybackEnded() async {
        // Save as completed
        await saveProgress(seconds: 0, duration: 0, completed: true)

        // Auto-play next if enabled
        if autoPlayNext && hasNext {
            await nextEpisode()
        }
    }

    // MARK: - Progress

    @MainActor
    func saveProgress(seconds: Int, duration: Int, completed: Bool = false) async {
        guard showId > 0 else { return }
        let episode = episodes.first(where: { $0.episodeNumber == episodeNumber })
        do {
            try await watchRepository.updateProgress(
                showId: showId,
                episodeNumber: episodeNumber,
                progressSeconds: seconds,
                durationSeconds: duration,
                completed: completed,
                episodeId: episode?.id
            )
        } catch {
            // Silently fail
        }
    }

    // MARK: - Subtitle Controls

    func toggleSubtitleEnabled() {
        subtitleEnabled.toggle()
    }

    func cycleSubtitleSize() {
        switch subtitleSize {
        case .small: subtitleSize = .medium
        case .medium: subtitleSize = .large
        case .large: subtitleSize = .small
        }
    }

    func toggleSubtitleBg() {
        subtitleBgEnabled.toggle()
    }

    // MARK: - Controls Visibility

    func showControlsTemporarily() {
        showControls = true
        controlsTimer?.cancel()
        controlsTimer = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 5_000_000_000) // 5 seconds
            guard !Task.isCancelled else { return }
            showControls = false
        }
    }

    func toggleControls() {
        if showControls {
            showControls = false
            controlsTimer?.cancel()
        } else {
            showControlsTemporarily()
        }
    }

    // MARK: - Subtitle Query

    func subtitle(at time: TimeInterval) -> SubtitleCue? {
        subtitleParser.cue(at: time)
    }

    // MARK: - Prefetch

    @MainActor
    func prefetchNextEpisode() async {
        let next = episodeNumber + 1
        guard let episode = episodes.first(where: { $0.episodeNumber == next }) else { return }
        // Just prefetch sources so they are cached
        _ = try? await showRepository.getEpisodeSources(episodeId: episode.id)
    }

    // MARK: - Private

    private func updateNavState() {
        let epNumbers = episodes.map { $0.episodeNumber }
        hasNext = epNumbers.contains(episodeNumber + 1)
        hasPrev = episodeNumber > 1 && epNumbers.contains(episodeNumber - 1)
    }
}

// MARK: - Subtitle Size

enum SubtitleSize: String, CaseIterable {
    case small
    case medium
    case large

    var fontSize: CGFloat {
        switch self {
        case .small: return 14
        case .medium: return 18
        case .large: return 24
        }
    }

    var displayName: String {
        rawValue.capitalized
    }
}
