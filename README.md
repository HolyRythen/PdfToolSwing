# 📚 PdfToolSwing

Ein modernes, reines **Java Swing‑Tool** zum **Zusammenfügen (Merge)** und **Aufteilen (Split)** von PDF‑Dateien.  
Einfach zu bedienen, leichtgewichtig und 100 % offline.

---

## 🚀 Features

- ✅ **PDFs mergen** — mehrere Dateien zu einer einzigen kombinieren  
- ✂️ **PDFs splitten** — in Einzelseiten oder bestimmte Seitenbereiche zerlegen  
- 📂 **Drag & Drop‑Support** (Dateien direkt auf die Liste ziehen)  
- 🧩 **Reihenfolge ändern** per Pfeiltasten oder Ziehen  
- 📁 **Dateinamen‑Normalisierung** beim Splitten (keine Sonderzeichen)  
- 💾 Fortschrittsanzeige & Statusleiste  
- 🔧 Kein Framework, nur **Swing + Apache PDFBox**

---

## 🧩 Voraussetzungen

- **Java 17 oder neuer**
- **Apache PDFBox** (z. B. `pdfbox-app-3.0.6.jar`)

Lade PDFBox von: [https://pdfbox.apache.org/download.html](https://pdfbox.apache.org/download.html)

---

## ⚙️ Installation & Start

1. Lege die Datei **`PdfToolSwing.java`** in einen Ordner, z. B.:  
   `C:\Users\RobertMartin\Desktop\java-programms\PDF Merger`

2. Lade **`pdfbox-app-3.0.6.jar`** herunter und speichere sie im selben Ordner.

3. Kompiliere das Programm:

   ```powershell
   javac -cp "pdfbox-app-3.0.6.jar" PdfToolSwing.java
   ```

4. Starte das Tool:

   ```powershell
   java -cp ".;pdfbox-app-3.0.6.jar" PdfToolSwing
   ```

> 💡 Auf Linux/macOS `:` statt `;` verwenden (z. B. `java -cp ".:pdfbox-app-3.0.6.jar" PdfToolSwing`).

---

## 🖥️ Nutzung

### 🔹 PDF-Dateien mergen
1. Ziehe mehrere `.pdf`‑Dateien in das Fenster **oder** klicke „Hinzufügen…“  
2. Sortiere sie in der gewünschten Reihenfolge (mit „↑“ / „↓“)  
3. Klicke **„Mergen →“**  
4. Wähle den Zielnamen, z. B. `merged.pdf`  
5. Fertig ✅ — die kombinierte Datei wird gespeichert.

### 🔹 PDF-Dateien splitten
1. Wähle eine einzelne Datei aus der Liste **oder** lade eine per Dialog  
2. Klicke **„Splitten“**  
3. Wähle den Modus:
   - „Einzelseiten (alle)“ → jede Seite als eigene PDF  
   - „Bereich(e)…“ → z. B. `1-3,7,10-12`  
4. Wähle den Zielordner  
5. Ergebnis: mehrere Dateien `dokument_p001.pdf`, `dokument_p002.pdf`, …

---

## 📊 Beispiele

| Aktion | Beschreibung |
|--------|---------------|
| **Mergen** | `file1.pdf` + `file2.pdf` → `merged.pdf` |
| **Splitten** | `input.pdf (12 Seiten)` → `input_p001.pdf` … `input_p012.pdf` |
| **Bereich (1-3,7)** | Extrahiert Seiten 1–3 & 7 als Einzel‑PDFs |

---

## ⚠️ Hinweise

- PDFBox verarbeitet nur **valide PDF‑Dateien** (keine passwortgeschützten PDFs).  
- Lange Dateinamen oder Umlaute werden beim Split optional automatisch bereinigt.  
- Bei sehr großen Dateien (200+ MB) kann der Vorgang einige Sekunden dauern.  

---

## 💡 Erweiterungsideen

- 🔐 Unterstützung für passwortgeschützte PDFs  
- 🖼 Vorschau der Seiten (Thumbnails)  
- 🧠 Merge‑Profile speichern (z. B. „Rechnungszusammenführung“)  
- 📁 CLI‑Modus (`java PdfToolSwing --merge file1.pdf file2.pdf`)  

---

## 📁 Lizenz

MIT License — frei nutzbar & veränderbar.

---

© 2025 Robert Martin
