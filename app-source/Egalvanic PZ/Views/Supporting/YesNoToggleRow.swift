import SwiftUI

struct YesNoToggleRow: View {
    let label: String
    @Binding var value: Bool
    var yesColor: Color = .green
    var noColor: Color = .red

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            HStack(spacing: 0) {
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        value = true
                    }
                } label: {
                    Text(AppStrings.Common.yes)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(value ? yesColor : Color.clear)
                        .foregroundColor(value ? .white : .secondary)
                }
                .buttonStyle(.borderless)

                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        value = false
                    }
                } label: {
                    Text(AppStrings.Common.no)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(!value ? noColor : Color.clear)
                        .foregroundColor(!value ? .white : .secondary)
                }
                .buttonStyle(.borderless)
            }
            .background(Color(.systemGray6))
            .cornerRadius(8)
        }
    }
}
