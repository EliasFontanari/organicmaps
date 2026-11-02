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

_(in corso)_

### File modificati

_(in corso)_

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

### Aperto

_(in corso)_
