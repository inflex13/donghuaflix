import SwiftUI

@main
struct DonghuaFlixApp: App {
    @State private var router = Router()
    @State private var apiClient = APIClient()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(router)
                .environment(apiClient)
                .preferredColorScheme(.dark)
        }
    }
}
