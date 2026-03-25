import SwiftUI

// MARK: - Color Extension (Hex Initializer)

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)

        let a, r, g, b: UInt64
        switch hex.count {
        case 6: // RGB
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - DonghuaFlix Theme

enum DonghuaFlixTheme {

    // MARK: - Background Colors

    static let obsidian = Color(hex: "07060B")
    static let deepVoid = Color(hex: "0D0B14")
    static let surfaceDark = Color(hex: "13111C")
    static let surfaceCard = Color(hex: "1A1726")
    static let surfaceCardHover = Color(hex: "231F33")

    // MARK: - Accent Colors

    static let accentPurple = Color(hex: "8B5CF6")
    static let accentFuchsia = Color(hex: "D946EF")
    static let accentPink = Color(hex: "EC4899")
    static let accentGold = Color(hex: "FBBF24")
    static let secondaryColor = Color(hex: "F97316")

    // MARK: - Text Colors

    static let textPrimary = Color(hex: "F1F0F5")
    static let textSecondary = Color(hex: "9CA3AF")
    static let textMuted = Color(hex: "6B7280")
    static let textAccent = Color(hex: "C084FC")

    // MARK: - Gradients

    static let backgroundGradient = LinearGradient(
        colors: [obsidian, deepVoid],
        startPoint: .top,
        endPoint: .bottom
    )

    static let accentGradient = LinearGradient(
        colors: [accentPurple, accentFuchsia],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let accentGradientVertical = LinearGradient(
        colors: [accentPurple, accentFuchsia],
        startPoint: .top,
        endPoint: .bottom
    )

    static let warmGradient = LinearGradient(
        colors: [accentPink, secondaryColor],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let goldGradient = LinearGradient(
        colors: [accentGold, secondaryColor],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let cardGradient = LinearGradient(
        colors: [surfaceCard, surfaceDark],
        startPoint: .top,
        endPoint: .bottom
    )

    static let fadeToBlackBottom = LinearGradient(
        colors: [.clear, obsidian.opacity(0.8), obsidian],
        startPoint: .top,
        endPoint: .bottom
    )

    static let fadeToBlackTop = LinearGradient(
        colors: [obsidian, obsidian.opacity(0.5), .clear],
        startPoint: .top,
        endPoint: .bottom
    )
}

// MARK: - View Modifiers

extension View {
    func donghuaFlixBackground() -> some View {
        self.background(DonghuaFlixTheme.backgroundGradient.ignoresSafeArea())
    }

    func cardStyle() -> some View {
        self
            .background(DonghuaFlixTheme.surfaceCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
