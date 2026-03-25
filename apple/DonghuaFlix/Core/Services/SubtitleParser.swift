import Foundation

// MARK: - Subtitle Cue

struct SubtitleCue: Identifiable, Equatable {
    let index: Int
    let startTime: TimeInterval
    let endTime: TimeInterval
    let text: String

    var id: Int { index }

    /// Duration of this cue in seconds
    var duration: TimeInterval {
        endTime - startTime
    }
}

// MARK: - Subtitle Parser

final class SubtitleParser {

    private var cues: [SubtitleCue] = []

    /// Sorted cues parsed from an SRT file
    var allCues: [SubtitleCue] { cues }

    // MARK: - Download and Parse

    /// Downloads SRT content from a URL and parses it
    func load(from url: URL) async throws {
        let (data, _) = try await URLSession.shared.data(from: url)
        guard let content = String(data: data, encoding: .utf8)
                ?? String(data: data, encoding: .ascii) else {
            throw SubtitleParserError.invalidEncoding
        }
        cues = Self.parse(srt: content)
    }

    /// Parse an SRT string directly
    func load(srt content: String) {
        cues = Self.parse(srt: content)
    }

    // MARK: - Query

    /// Get the subtitle cue that should be displayed at the given time
    func cue(at time: TimeInterval) -> SubtitleCue? {
        // Binary search for performance with large subtitle files
        var low = 0
        var high = cues.count - 1

        while low <= high {
            let mid = (low + high) / 2
            let c = cues[mid]

            if time < c.startTime {
                high = mid - 1
            } else if time > c.endTime {
                low = mid + 1
            } else {
                return c
            }
        }
        return nil
    }

    /// Get all cues within a time range
    func cues(in range: ClosedRange<TimeInterval>) -> [SubtitleCue] {
        cues.filter { $0.startTime <= range.upperBound && $0.endTime >= range.lowerBound }
    }

    // MARK: - SRT Parsing

    static func parse(srt content: String) -> [SubtitleCue] {
        var cues: [SubtitleCue] = []

        // Normalize line endings
        let normalized = content
            .replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")

        // Split into blocks separated by blank lines
        let blocks = normalized.components(separatedBy: "\n\n")

        for block in blocks {
            let lines = block.trimmingCharacters(in: .whitespacesAndNewlines)
                .components(separatedBy: "\n")

            guard lines.count >= 3 else { continue }

            // First line: index number
            guard let index = Int(lines[0].trimmingCharacters(in: .whitespaces)) else { continue }

            // Second line: timestamps (00:00:00,000 --> 00:00:00,000)
            let timeLine = lines[1]
            let timeParts = timeLine.components(separatedBy: " --> ")
            guard timeParts.count == 2 else { continue }

            guard let startTime = parseTimestamp(timeParts[0].trimmingCharacters(in: .whitespaces)),
                  let endTime = parseTimestamp(timeParts[1].trimmingCharacters(in: .whitespaces)) else {
                continue
            }

            // Remaining lines: subtitle text
            let text = lines[2...].joined(separator: "\n")
                .trimmingCharacters(in: .whitespacesAndNewlines)

            // Strip HTML-like tags (e.g., <i>, <b>, <font>)
            let cleanText = text.replacingOccurrences(
                of: "<[^>]+>",
                with: "",
                options: .regularExpression
            )

            guard !cleanText.isEmpty else { continue }

            cues.append(SubtitleCue(
                index: index,
                startTime: startTime,
                endTime: endTime,
                text: cleanText
            ))
        }

        return cues.sorted { $0.startTime < $1.startTime }
    }

    /// Parse an SRT timestamp string into TimeInterval
    /// Format: HH:MM:SS,mmm or HH:MM:SS.mmm
    private static func parseTimestamp(_ string: String) -> TimeInterval? {
        // Replace comma with period for consistent parsing
        let normalized = string.replacingOccurrences(of: ",", with: ".")

        let parts = normalized.components(separatedBy: ":")
        guard parts.count == 3 else { return nil }

        guard let hours = Double(parts[0]),
              let minutes = Double(parts[1]),
              let seconds = Double(parts[2]) else {
            return nil
        }

        return hours * 3600 + minutes * 60 + seconds
    }
}

// MARK: - Errors

enum SubtitleParserError: LocalizedError {
    case invalidEncoding
    case downloadFailed

    var errorDescription: String? {
        switch self {
        case .invalidEncoding:
            return "Could not decode subtitle file"
        case .downloadFailed:
            return "Failed to download subtitle file"
        }
    }
}
