import SwiftUI
import AVFoundation
import AVKit

struct PlayerView: View {
    let showId: Int
    let episodeNumber: Int
    let website: String?

    @Environment(ShowRepository.self) private var showRepository
    @Environment(WatchRepository.self) private var watchRepository
    @Environment(Router.self) private var router

    @State private var viewModel: PlayerViewModel?
    @State private var player: AVPlayer?
    @State private var isPlaying = false
    @State private var currentTime: TimeInterval = 0
    @State private var duration: TimeInterval = 0
    @State private var currentSubtitle: SubtitleCue?
    @State private var seekPosition: Double = 0
    @State private var isSeeking = false
    @State private var timeObserver: Any?
    @State private var progressSaveTask: Task<Void, Never>?

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if let vm = viewModel {
                if vm.isLoading {
                    loadingView
                } else if let error = vm.error {
                    errorView(error)
                } else {
                    playerContent(vm: vm)
                }
            } else {
                loadingView
            }
        }
        .ignoresSafeArea()
        #if os(iOS)
        .navigationBarHidden(true)
        .statusBarHidden(viewModel?.showControls == false)
        .persistentSystemOverlays(.hidden)
        #endif
        #if os(macOS)
        .onDisappear {
            // Exit fullscreen when leaving player
            if let window = NSApplication.shared.keyWindow,
               window.styleMask.contains(.fullScreen) {
                window.toggleFullScreen(nil)
            }
        }
        #endif
        .task {
            if viewModel == nil {
                viewModel = PlayerViewModel(
                    showRepository: showRepository,
                    watchRepository: watchRepository
                )
            }
            await viewModel?.loadPlayer(showId: showId, episodeNumber: episodeNumber, website: website)
            setupPlayer()
        }
        .onDisappear {
            cleanupPlayer()
        }
    }

    // MARK: - Player Content

    @ViewBuilder
    private func playerContent(vm: PlayerViewModel) -> some View {
        ZStack {
            // Video layer
            if let player = player {
                VideoPlayerView(player: player)
                    .ignoresSafeArea()
            }

            // Tap area for toggling controls
            Color.clear
                .contentShape(Rectangle())
                .onTapGesture {
                    vm.toggleControls()
                }

            // Subtitle overlay
            if vm.subtitleEnabled, let cue = currentSubtitle {
                VStack {
                    Spacer()
                    subtitleOverlay(cue: cue, vm: vm)
                        .padding(.bottom, vm.showControls ? 100 : 40)
                }
            }

            // Controls overlay
            if vm.showControls {
                controlsOverlay(vm: vm)
            }
        }
        #if os(macOS)
        .onKeyPress(.space) {
            togglePlayPause()
            return .handled
        }
        .onKeyPress(.leftArrow) {
            seek(by: -10)
            return .handled
        }
        .onKeyPress(.rightArrow) {
            seek(by: 10)
            return .handled
        }
        .onKeyPress(characters: CharacterSet(charactersIn: "c")) { _ in
            vm.toggleSubtitleEnabled()
            return .handled
        }
        #endif
    }

    // MARK: - Controls Overlay

    @ViewBuilder
    private func controlsOverlay(vm: PlayerViewModel) -> some View {
        ZStack {
            // Top gradient
            VStack {
                LinearGradient(
                    colors: [Color.black.opacity(0.7), .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 100)
                Spacer()
            }
            .ignoresSafeArea()

            // Bottom gradient
            VStack {
                Spacer()
                LinearGradient(
                    colors: [.clear, Color.black.opacity(0.8)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 160)
            }
            .ignoresSafeArea()

            VStack {
                // Top bar: back, title, settings
                topBar(vm: vm)

                Spacer()

                // Center: play/pause + seek buttons
                centerControls

                Spacer()

                // Bottom: seekbar + action row
                bottomControls(vm: vm)
            }
            .padding()
        }
        .transition(.opacity)
        .animation(.easeInOut(duration: 0.25), value: vm.showControls)
    }

    // MARK: - Top Bar

    @ViewBuilder
    private func topBar(vm: PlayerViewModel) -> some View {
        HStack {
            Button {
                cleanupPlayer()
                router.pop()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.title3.bold())
                    .foregroundStyle(.white)
                    .padding(10)
                    .background(Circle().fill(Color.white.opacity(0.15)))
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 2) {
                Text(vm.showTitle)
                    .font(.subheadline.bold())
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Text("Episode \(vm.episodeNumber)")
                    .font(.caption)
                    .foregroundStyle(Color.white.opacity(0.7))
            }

            Spacer()

            // Source selector
            if vm.sources.count > 1 {
                Menu {
                    ForEach(vm.sources) { source in
                        Button {
                            Task {
                                await vm.selectSource(source)
                                setupPlayer()
                            }
                        } label: {
                            HStack {
                                Text(source.sourceName)
                                if source.id == vm.selectedSource?.id {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "server.rack")
                            .font(.caption)
                        Text(vm.selectedSource?.sourceName ?? "Source")
                            .font(.caption.bold())
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Capsule().fill(Color.white.opacity(0.15)))
                }
            }
        }
    }

    // MARK: - Center Controls

    @ViewBuilder
    private var centerControls: some View {
        HStack(spacing: 48) {
            // Seek backward
            Button { seek(by: -10) } label: {
                Image(systemName: "gobackward.10")
                    .font(.title)
                    .foregroundStyle(.white)
            }
            .buttonStyle(.plain)

            // Play/Pause
            Button { togglePlayPause() } label: {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 44))
                    .foregroundStyle(.white)
            }
            .buttonStyle(.plain)

            // Seek forward
            Button { seek(by: 10) } label: {
                Image(systemName: "goforward.10")
                    .font(.title)
                    .foregroundStyle(.white)
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Bottom Controls

    @ViewBuilder
    private func bottomControls(vm: PlayerViewModel) -> some View {
        VStack(spacing: 10) {
            // Seek bar
            seekBar

            // Action row
            HStack(spacing: 16) {
                // Time display
                Text("\(formatTime(currentTime)) / \(formatTime(duration))")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(Color.white.opacity(0.7))

                Spacer()

                // Subtitle toggle
                Button { vm.toggleSubtitleEnabled() } label: {
                    Image(systemName: vm.subtitleEnabled ? "captions.bubble.fill" : "captions.bubble")
                        .font(.subheadline)
                        .foregroundStyle(vm.subtitleEnabled ? DonghuaFlixTheme.accentFuchsia : .white)
                }
                .buttonStyle(.plain)

                // Subtitle size
                Button { vm.cycleSubtitleSize() } label: {
                    Text(vm.subtitleSize.displayName.prefix(1))
                        .font(.caption.bold())
                        .foregroundStyle(.white)
                        .frame(width: 24, height: 24)
                        .background(Circle().fill(Color.white.opacity(0.15)))
                }
                .buttonStyle(.plain)

                // Subtitle background
                Button { vm.toggleSubtitleBg() } label: {
                    Image(systemName: vm.subtitleBgEnabled ? "rectangle.fill" : "rectangle")
                        .font(.caption)
                        .foregroundStyle(.white)
                }
                .buttonStyle(.plain)

                // Prev episode
                if vm.hasPrev {
                    Button {
                        Task {
                            cleanupPlayer()
                            await vm.previousEpisode()
                            setupPlayer()
                        }
                    } label: {
                        Image(systemName: "backward.end.fill")
                            .font(.subheadline)
                            .foregroundStyle(.white)
                    }
                    .buttonStyle(.plain)
                }

                // Next episode
                if vm.hasNext {
                    Button {
                        Task {
                            cleanupPlayer()
                            await vm.nextEpisode()
                            setupPlayer()
                        }
                    } label: {
                        Image(systemName: "forward.end.fill")
                            .font(.subheadline)
                            .foregroundStyle(.white)
                    }
                    .buttonStyle(.plain)
                }

                // AirPlay button
                #if os(iOS)
                AirPlayButton()
                    .frame(width: 28, height: 28)
                #endif

                // Fullscreen toggle
                #if os(macOS)
                Button {
                    if let window = NSApplication.shared.keyWindow {
                        window.toggleFullScreen(nil)
                    }
                } label: {
                    Image(systemName: "arrow.up.left.and.arrow.down.right")
                        .font(.subheadline)
                        .foregroundStyle(.white)
                }
                .buttonStyle(.plain)
                #endif

                // Autoplay toggle
                Button {
                    vm.autoPlayNext.toggle()
                } label: {
                    HStack(spacing: 3) {
                        Image(systemName: vm.autoPlayNext ? "play.circle.fill" : "play.circle")
                            .font(.caption)
                        Text("Auto")
                            .font(.caption2)
                    }
                    .foregroundStyle(vm.autoPlayNext ? DonghuaFlixTheme.accentFuchsia : .white)
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Seek Bar

    @ViewBuilder
    private var seekBar: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                // Background track
                Capsule()
                    .fill(Color.white.opacity(0.2))
                    .frame(height: 4)

                // Progress
                Capsule()
                    .fill(DonghuaFlixTheme.accentFuchsia)
                    .frame(width: max(0, geo.size.width * progressFraction), height: 4)

                // Thumb
                Circle()
                    .fill(DonghuaFlixTheme.accentFuchsia)
                    .frame(width: 14, height: 14)
                    .offset(x: max(0, geo.size.width * progressFraction - 7))
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        isSeeking = true
                        let fraction = max(0, min(1, value.location.x / geo.size.width))
                        seekPosition = fraction * duration
                        currentTime = seekPosition
                    }
                    .onEnded { _ in
                        player?.seek(to: CMTime(seconds: seekPosition, preferredTimescale: 600))
                        isSeeking = false
                    }
            )
        }
        .frame(height: 14)
    }

    private var progressFraction: Double {
        guard duration > 0 else { return 0 }
        return currentTime / duration
    }

    // MARK: - Subtitle Overlay

    @ViewBuilder
    private func subtitleOverlay(cue: SubtitleCue, vm: PlayerViewModel) -> some View {
        Text(cue.text)
            .font(.system(size: vm.subtitleSize.fontSize, weight: .medium))
            .foregroundStyle(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                vm.subtitleBgEnabled
                    ? AnyShapeStyle(Color.black.opacity(0.7))
                    : AnyShapeStyle(Color.clear)
            )
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .padding(.horizontal, 20)
    }

    // MARK: - Player Management

    private func setupPlayer() {
        guard let vm = viewModel, let urlString = vm.streamUrl, let url = URL(string: urlString) else { return }

        let playerItem = AVPlayerItem(url: url)

        if let existingPlayer = player {
            existingPlayer.replaceCurrentItem(with: playerItem)
        } else {
            player = AVPlayer(playerItem: playerItem)
        }

        // Resume position
        if vm.resumePositionMs > 0 {
            let time = CMTime(value: CMTimeValue(vm.resumePositionMs), timescale: 1000)
            player?.seek(to: time)
        }

        player?.allowsExternalPlayback = true
        #if os(iOS)
        player?.usesExternalPlaybackWhileExternalScreenIsActive = true
        #endif
        player?.play()
        isPlaying = true
        vm.showControlsTemporarily()

        // Keep screen awake
        #if os(iOS)
        UIApplication.shared.isIdleTimerDisabled = true
        #endif

        addTimeObserver()
        observePlaybackEnd()
    }

    private func cleanupPlayer() {
        if let observer = timeObserver {
            player?.removeTimeObserver(observer)
            timeObserver = nil
        }
        progressSaveTask?.cancel()
        player?.pause()
        player = nil
        isPlaying = false

        #if os(iOS)
        UIApplication.shared.isIdleTimerDisabled = false
        #endif
    }

    private func addTimeObserver() {
        if let observer = timeObserver {
            player?.removeTimeObserver(observer)
        }

        let interval = CMTime(seconds: 0.5, preferredTimescale: 600)
        timeObserver = player?.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [self] time in
            guard !isSeeking else { return }

            let seconds = time.seconds
            guard seconds.isFinite else { return }
            currentTime = seconds

            if let dur = player?.currentItem?.duration.seconds, dur.isFinite {
                duration = dur
            }

            // Update subtitle
            if let vm = viewModel, vm.subtitleEnabled {
                currentSubtitle = vm.subtitle(at: seconds)
            } else {
                currentSubtitle = nil
            }

            // Save progress every 10 seconds
            let progressSeconds = Int(seconds)
            if progressSeconds > 0 && progressSeconds % 10 == 0 {
                progressSaveTask?.cancel()
                progressSaveTask = Task {
                    await viewModel?.saveProgress(
                        seconds: progressSeconds,
                        duration: Int(duration),
                        completed: false
                    )
                }
            }
        }
    }

    private func observePlaybackEnd() {
        NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: player?.currentItem,
            queue: .main
        ) { _ in
            Task { @MainActor in
                isPlaying = false
                await viewModel?.onPlaybackEnded()
                if let vm = viewModel, !vm.isLoading, vm.streamUrl != nil {
                    setupPlayer()
                }
            }
        }
    }

    // MARK: - Controls

    private func togglePlayPause() {
        guard let player = player else { return }
        if isPlaying {
            player.pause()
        } else {
            player.play()
        }
        isPlaying.toggle()
        viewModel?.showControlsTemporarily()
    }

    private func seek(by seconds: Double) {
        guard let player = player else { return }
        let newTime = max(0, currentTime + seconds)
        player.seek(to: CMTime(seconds: newTime, preferredTimescale: 600))
        currentTime = newTime
        viewModel?.showControlsTemporarily()
    }

    // MARK: - Formatting

    private func formatTime(_ time: TimeInterval) -> String {
        guard time.isFinite && time >= 0 else { return "0:00" }
        let totalSeconds = Int(time)
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60

        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    // MARK: - States

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(DonghuaFlixTheme.accentFuchsia)
                .scaleEffect(1.5)
            Text("Loading player...")
                .foregroundStyle(.white.opacity(0.7))
        }
    }

    @ViewBuilder
    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundStyle(DonghuaFlixTheme.accentGold)
            Text("Playback Error")
                .font(.title2.bold())
                .foregroundStyle(.white)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.7))
                .multilineTextAlignment(.center)

            HStack(spacing: 12) {
                Button("Go Back") {
                    router.pop()
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(DonghuaFlixTheme.surfaceCard)
                .clipShape(RoundedRectangle(cornerRadius: 8))

                Button("Retry") {
                    Task {
                        await viewModel?.loadPlayer(
                            showId: showId,
                            episodeNumber: episodeNumber,
                            website: website
                        )
                        setupPlayer()
                    }
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(DonghuaFlixTheme.accentGradient)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .padding()
    }
}

// MARK: - Video Player Representable

#if os(iOS)
struct VideoPlayerView: UIViewControllerRepresentable {
    let player: AVPlayer

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = false
        controller.videoGravity = .resizeAspect
        return controller
    }

    func updateUIViewController(_ uiViewController: AVPlayerViewController, context: Context) {
        uiViewController.player = player
    }
}
#elseif os(macOS)
struct VideoPlayerView: NSViewRepresentable {
    let player: AVPlayer

    func makeNSView(context: Context) -> NSView {
        let view = NSView()
        let playerLayer = AVPlayerLayer(player: player)
        playerLayer.videoGravity = .resizeAspect
        playerLayer.autoresizingMask = [.layerWidthSizable, .layerHeightSizable]
        view.wantsLayer = true
        view.layer?.addSublayer(playerLayer)
        return view
    }

    func updateNSView(_ nsView: NSView, context: Context) {
        if let playerLayer = nsView.layer?.sublayers?.first as? AVPlayerLayer {
            playerLayer.player = player
        }
    }
}
#endif

// MARK: - AirPlay Button

#if os(iOS)
import AVRouting

struct AirPlayButton: UIViewRepresentable {
    func makeUIView(context: Context) -> AVRoutePickerView {
        let picker = AVRoutePickerView()
        picker.tintColor = .white
        picker.activeTintColor = UIColor(DonghuaFlixTheme.accentFuchsia)
        picker.prioritizesVideoDevices = true
        return picker
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {}
}
#endif
