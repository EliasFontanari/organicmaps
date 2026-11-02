# CHANGES — fork personale

Registro delle modifiche rispetto a `upstream/master`, una sezione per feature.
Vedi `CLAUDE.md` per contesto e metodo di lavoro.

---

## F2 — Avviso memoria sotto soglia critica

Branch: `feature/low-storage-warning`

### Comportamento

- **All'avvio** (`MwmActivity.onResume`): se lo spazio libero nella cartella delle
  mappe è sotto soglia, dialog di avviso, una sola volta per processo.
- **Prima di avviare una registrazione traccia**: dialog sempre presente che mostra
  lo spazio libero e chiede conferma. Se lo spazio è sotto soglia lo segnala nel
  testo. Nessun controllo *durante* la registrazione (scelta esplicita).
- **Prima del download di una mappa**: se il download lascerebbe meno della soglia,
  avviso con possibilità di procedere o annullare. Il blocco "Not enough space"
  esistente (core, margine 10 MB) resta invariato.
- **Impostazioni**: soglia selezionabile fra Off / 200 MB / 500 MB / 1 GB / 2 GB,
  default 500 MB.

### File aggiunti

- `android/app/src/main/java/app/organicmaps/util/StorageSpaceMonitor.java` —
  logica di F2: spazio libero nella cartella delle mappe, soglia in
  `MwmApplication.prefs()` (chiave `LowStorageThresholdMb`, MB decimali, default
  500, 0 = Off), avviso all'avvio al massimo una volta per processo, formattazione.
- `android/app/src/main/res/values/strings_fork.xml` — tutte le stringhe del fork
  (solo inglese, `translatable="false"`) e gli array della preferenza soglia.

### File modificati

- `android/app/src/main/res/xml/prefs_main.xml` — categoria `pref_settings_general`,
  nuova `ListPreference` `pref_low_storage_threshold` subito dopo `pref_storage`
  (`order="9"`), `persistent="false"`.
- `android/app/src/main/java/app/organicmaps/settings/SettingsPrefsFragment.java` —
  `onViewCreated()`: chiamata a `initLowStorageThresholdPrefsCallbacks()`; nuovo
  metodo omonimo che legge e scrive la soglia tramite `StorageSpaceMonitor`.
- `android/app/src/main/java/app/organicmaps/MwmActivity.java`:
  - `onResume()`: una riga, `showLowStorageWarningIfNeeded()`; nuovo metodo omonimo
    che usa lo slot `mAlertDialog` esistente (non si sovrappone ad altri dialog).
  - nuovo `confirmAndStartTrackRecording()`: dialog sempre presente con lo spazio
    libero, riga aggiuntiva se sotto soglia, Cancel/Start. Chiamato da
    `onTrackRecordingOptionSelected()` e da `processIntent()`
    (`EXTRA_START_TRACK_RECORDING`) al posto di `startTrackRecording()`.
- `android/app/src/main/java/app/organicmaps/downloader/MapManagerHelper.java` —
  nuovi `warnLowSpaceAndOn3g()`, `getDownloadSize()`, `getUpdateSize()`;
  `warnOn3gUpdate()`, `warnOn3g(countryId)` e `warnOn3g(size)` passano da
  `warnLowSpaceAndOn3g()` invece che direttamente da `warnOn3gInternal()`.
  Ordine: blocco "Not enough space" del core → avviso soglia F2 → avviso dati mobili.

### Decisioni

- **Soglia fissa, non percentuale.** Il 5% di un telefono da 256 GB sono 12,8 GB:
  avviserebbe troppo presto. Conta lo spazio assoluto (una regione, qualche ora di
  tracce).
- **Spazio letto in Java** con `File.getUsableSpace()` sulla cartella restituita da
  `Framework.nativeGetWritableDir()`, cioè dove finiscono davvero mappe e tracce
  (anche se su SD). Nessuna modifica a `libs/`.
- **Stringhe in `values/strings_fork.xml`** invece che in `strings.xml`, che upstream
  modifica di continuo: riduce i conflitti nei rebase. Solo inglese, niente traduzioni.
- **Scartato: notifica durante la registrazione.** Sostituita dalla conferma prima
  dell'avvio, su richiesta.
- **Il dialog di conferma non compare** quando `startTrackRecording()` riprende una
  registrazione già attiva (creazione dell'activity) né dopo la concessione del
  permesso di posizione: in quel caso l'utente ha già confermato.
- **Soglia in `MwmApplication.prefs()`** e non nelle SharedPreferences di default
  della schermata impostazioni: stesso schema delle altre preferenze dell'app.
- **Avviso download agganciato in `MapManagerHelper`** e non nei singoli chiamanti
  (downloader, place page, banner sulla mappa, errore di routing): tutti passano
  dai tre metodi `warnOn3g*`.

### Aperto

- **Verifica su dispositivo da fare.** Finora solo compilazione Java. Prova
  prevista: impostare la soglia a 2 GB (o sopra lo spazio libero), poi controllare
  dialog all'avvio, conferma prima della registrazione con la riga "below the
  threshold", e avviso prima del download di una regione. Con soglia Off: niente
  dialog all'avvio né prima del download; la conferma di registrazione resta, senza
  la riga di avviso.
- Stringhe solo in inglese.
