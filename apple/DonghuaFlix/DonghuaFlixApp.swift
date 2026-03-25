import SwiftUI

@main
struct DonghuaFlixApp: App {
    @State private var router = Router()
    @State private var apiClient = APIClient()
    @State private var showRepository: ShowRepository
    @State private var watchRepository: WatchRepository

    init() {
        let client = APIClient()
        _apiClient = State(initialValue: client)
        _router = State(initialValue: Router())
        _showRepository = State(initialValue: ShowRepository(apiClient: client))
        _watchRepository = State(initialValue: WatchRepository(apiClient: client))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(router)
                .environment(apiClient)
                .environment(showRepository)
                .environment(watchRepository)
                .preferredColorScheme(.dark)
        }
    }
}
