import SwiftUI
import UIKit

/// A UIViewRepresentable wrapper around UIScrollView for zooming and panning
struct ZoomableScrollView<Content: View>: UIViewRepresentable {
    var content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    func makeUIView(context: Context) -> UIScrollView {
        let scrollView = UIScrollView()
        scrollView.delegate = context.coordinator
        scrollView.minimumZoomScale = 1.0
        scrollView.maximumZoomScale = 5.0
        scrollView.showsVerticalScrollIndicator = false
        scrollView.showsHorizontalScrollIndicator = false
        // Host the SwiftUI view
        let hosted = UIHostingController(rootView: content)
        hosted.view.translatesAutoresizingMaskIntoConstraints = false
        hosted.view.backgroundColor = .clear
        scrollView.addSubview(hosted.view)
        // Constraints
        NSLayoutConstraint.activate([
            hosted.view.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor),
            hosted.view.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor),
            hosted.view.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            hosted.view.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),
            hosted.view.widthAnchor.constraint(equalTo: scrollView.frameLayoutGuide.widthAnchor),
            hosted.view.heightAnchor.constraint(equalTo: scrollView.frameLayoutGuide.heightAnchor)
        ])
        return scrollView
    }

    func updateUIView(_ uiView: UIScrollView, context: Context) {
        // nothing
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    class Coordinator: NSObject, UIScrollViewDelegate {
        func viewForZooming(in scrollView: UIScrollView) -> UIView? {
            return scrollView.subviews.first
        }
    }
}

struct FullImageView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @State private var currentIndex: Int
    @State private var showCaptionEditor = false
    @State private var editingCaption = ""
    let photos: [Photo]

    init(selected: Photo, allPhotos: [Photo]) {
        _currentIndex = State(initialValue: allPhotos.firstIndex(where: { $0.id == selected.id }) ?? 0)
        self.photos = allPhotos
    }

    var currentPhoto: Photo {
        photos[currentIndex]
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                // Close button
                HStack {
                    Spacer()
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.white)
                    }
                    .padding()
                }

                // Photo TabView
                TabView(selection: $currentIndex) {
                ForEach(Array(photos.enumerated()), id: \ .element.id) { idx, photo in
                    ZoomableScrollView {
                        PresignedPhotoImage(
                            photo: photo,
                            content: { image in
                                image
                                    .resizable()
                                    .scaledToFit()
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            },
                            placeholder: {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(2.0)
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            },
                            onFailure: {
                                Image(systemName: "xmark.octagon")
                                    .resizable()
                                    .scaledToFit()
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            },
                            retryButtonAlignment: .center
                        )
                    }
                    .tag(idx)
                }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))

                // Caption display panel (if caption exists)
                if let caption = currentPhoto.caption, !caption.isEmpty {
                    CaptionDisplayPanel(
                        caption: caption,
                        onEditTap: {
                            editingCaption = currentPhoto.caption ?? ""
                            showCaptionEditor = true
                        }
                    )
                }
            }
        }
        .gesture(DragGesture().onEnded { value in
            if value.translation.width < -50 {
                // Swipe Left
                withAnimation {
                    currentIndex = min(currentIndex + 1, photos.count - 1)
                }
            } else if value.translation.width > 50 {
                // Swipe Right
                withAnimation {
                    currentIndex = max(currentIndex - 1, 0)
                }
            }
        })
        .sheet(isPresented: $showCaptionEditor) {
            CaptionEditingView(
                caption: $editingCaption,
                onSave: {
                    PhotoService.updatePhotoCaption(
                        currentPhoto,
                        caption: editingCaption,
                        modelContext: modelContext
                    ) { success, message in
                        if success {
                            AppLogger.log(.info, "Caption update: \(message ?? "Success")", category: .photo)
                        } else {
                            AppLogger.log(.error, "Caption update failed: \(message ?? "Unknown error")", category: .photo)
                        }
                    }
                },
                onCancel: {
                    // Revert changes
                    editingCaption = currentPhoto.caption ?? ""
                }
            )
        }
    }
}
