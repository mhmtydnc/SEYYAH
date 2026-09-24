import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { api } from '../api/istemci'
import type { Konum, KonumOnerisi } from '../api/tipler'

interface Ozellikler {
  etiket: string
  konum: Konum | null
  onSec: (konum: Konum | null) => void
}

export function KonumAlani({ etiket, konum, onSec }: Ozellikler) {
  const [metin, metinAyarla] = useState(konum?.ad ?? '')
  const [oneriler, onerilerAyarla] = useState<KonumOnerisi[]>([])
  const [acik, acikAyarla] = useState(false)
  const [etkinSira, etkinSiraAyarla] = useState(-1)
  const [hata, hataAyarla] = useState('')
  const listeKimligi = useId()
  const secildi = useRef(false)

  useEffect(() => {
    if (konum) {
      metinAyarla(konum.ad)
      secildi.current = true
      onerilerAyarla([])
      acikAyarla(false)
    }
  }, [konum])

  useEffect(() => {
    if (secildi.current || metin.trim().length < 2) {
      onerilerAyarla([])
      return
    }
    const denetleyici = new AbortController()
    const zamanlayici = window.setTimeout(() => {
      api.konumAra(metin.trim(), denetleyici.signal)
        .then((sonuclar) => { onerilerAyarla(sonuclar); acikAyarla(true); hataAyarla('') })
        .catch((neden: unknown) => {
          if (neden instanceof Error && neden.name === 'AbortError') return
          hataAyarla(neden instanceof Error ? neden.message : 'Konum aranamadı.')
          onerilerAyarla([])
        })
    }, 300)
    return () => { window.clearTimeout(zamanlayici); denetleyici.abort() }
  }, [metin])

  function sec(onerilen: KonumOnerisi) {
    secildi.current = true
    onSec({ ad: onerilen.ad, enlem: onerilen.enlem, boylam: onerilen.boylam })
    metinAyarla(onerilen.etiket)
    acikAyarla(false)
    etkinSiraAyarla(-1)
  }

  function tus(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Escape') { acikAyarla(false); return }
    if (!acik || !oneriler.length) return
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      etkinSiraAyarla((sira) => (sira + 1) % oneriler.length)
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      etkinSiraAyarla((sira) => (sira + oneriler.length - 1) % oneriler.length)
    } else if (event.key === 'Enter' && etkinSira >= 0) {
      event.preventDefault()
      sec(oneriler[etkinSira])
    }
  }

  return <div className="konum-alani">
    <label htmlFor={listeKimligi}>{etiket}</label>
    <input id={listeKimligi} role="combobox" aria-expanded={acik && oneriler.length > 0}
      aria-controls={`${listeKimligi}-liste`} aria-autocomplete="list"
      aria-activedescendant={etkinSira >= 0 && acik ? `${listeKimligi}-${etkinSira}` : undefined}
      value={metin} placeholder={`${etiket} konumu ara`} autoComplete="off"
      onChange={(event) => {
        secildi.current = false
        metinAyarla(event.target.value)
        onSec(null)
        acikAyarla(false)
        etkinSiraAyarla(-1)
        hataAyarla('')
      }}
      onFocus={() => { if (oneriler.length) acikAyarla(true) }}
      onBlur={() => window.setTimeout(() => acikAyarla(false), 150)}
      onKeyDown={tus} />
    {acik && oneriler.length > 0 && <ul id={`${listeKimligi}-liste`} role="listbox" className="oneriler">
      {oneriler.map((onerilen, sira) => <li key={`${onerilen.enlem}-${onerilen.boylam}-${sira}`}
        id={`${listeKimligi}-${sira}`} role="option" aria-selected={etkinSira === sira}>
        <button type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => sec(onerilen)}>{onerilen.etiket}</button>
      </li>)}
    </ul>}
    {hata && <small role="alert" className="hata">{hata}</small>}
  </div>
}
