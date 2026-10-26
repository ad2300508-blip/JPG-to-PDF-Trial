# Project Context & Rules

## Tech Stack
- **Linguaggio:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Architettura:** MVVM (ViewModel + StateFlow)
- **Database:** Room SQLite

## Mappa dei File (Usa questi percorsi, NON fare ricerche globali)
- **Tema e Colori:** `app/src/main/java/com/tuopackage/ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`)
- **Componenti Condivisi:** `app/src/main/java/com/tuopackage/ui/components/`
- **Schermate (Screens):** `app/src/main/java/com/tuopackage/ui/screens/`
- **Navigazione:** `app/src/main/java/com/tuopackage/ui/navigation/NavGraph.kt`
- **Database/DAO:** `app/src/main/java/com/tuopackage/data/local/`

## Regole Design System (Sostituisce i PDF di design)
- **Background Light / Dark:** `#FFFFFF` / `#121212`
- **Primary / Accent:** `#6200EE` / `#03DAC5`
- **Stile:** Usa sempre `MaterialTheme.colorScheme` e `MaterialTheme.typography`. Non usare colori hardcoded (`Color(0xFF...)`) dentro le singole schermate.

## Regole di Risparmio Token per Claude (CRITICO)
1. **Zero esplorazione cieca:** Vai dritto ai file indicati nella mappa. Chiedi il percorso se non sai dove si trova un file, invece di scansionare l'intero progetto.
2. **Niente build automatiche:** NON eseguire `./gradlew build` o `./gradlew assembleDebug` dopo ogni modifica (i log di Gradle consumano troppi token). Eseguili solo se te lo chiedo esplicitamente.
3. **Risposte sintetiche:** Modifica il codice senza rispiegare cosa hai fatto. Limitati a un elenco puntato di 2 righe a fine lavoro.
4. **File grandi:** Usa `grep` o leggi solo i range di righe necessari invece di caricare interi file superiori a 300 righe.
