// ARMOR-WATCH - watchos/App/ContentView.swift
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
import ArmorWatchCore
import SwiftUI

private func colour(_ severity: Severity) -> Color {
    switch severity {
    case .critical: return Color(red: 0.93, green: 0.42, blue: 0.50)
    case .warning: return Color(red: 0.95, green: 0.73, blue: 0.33)
    case .info: return Color(red: 0.22, green: 0.83, blue: 0.90)
    }
}

struct ContentView: View {
    @ObservedObject var model: WatchModel

    var body: some View {
        switch model.screen {
        case .off:
            Message(key: "alerts_off")
        case .quiet:
            Message(key: "no_alerts")
        case .alerts(let lines):
            List {
                ForEach(lines, id: \.alertId) { line in
                    VStack(spacing: 4) {
                        Text(line.title).font(.headline).foregroundColor(colour(line.severity)).multilineTextAlignment(.center)
                        Text(line.text).font(.footnote).multilineTextAlignment(.center)
                        if line.confirmed {
                            Text("confirmed").font(.footnote).foregroundColor(colour(.info))
                        } else if line.canConfirm {
                            Button("confirm") { model.confirm(line.alertId) }
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
                if model.linkDown {
                    Text("phone_out_of_reach").font(.footnote).foregroundColor(colour(.warning)).multilineTextAlignment(.center)
                }
            }
        }
    }
}

private struct Message: View {
    let key: LocalizedStringKey

    var body: some View {
        Text(key).multilineTextAlignment(.center).padding()
    }
}
