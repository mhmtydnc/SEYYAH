import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, IstekHatasi, TOKEN_ANAHTARI } from '../api/istemci'
import type { Kullanici } from '../api/tipler'

interface OturumDegeri {
  kullanici: Kullanici | null
  hazir: boolean
  girisYap: (eposta: string, sifre: string) => Promise<void>
  kayitOl: (ad: string, eposta: string, sifre: string) => Promise<void>
  cikisYap: () => void
}

const OturumBaglami = createContext<OturumDegeri | null>(null)

export function OturumSaglayici({ children }: { children: ReactNode }) {
  const [kullanici, kullaniciAyarla] = useState<Kullanici | null>(null)
  const [hazir, hazirAyarla] = useState(!localStorage.getItem(TOKEN_ANAHTARI))

  useEffect(() => {
    if (!localStorage.getItem(TOKEN_ANAHTARI)) return
    let etkin = true
    api.ben().then((gelen) => {
      if (etkin) kullaniciAyarla(gelen)
    }).catch((hata: unknown) => {
      if (hata instanceof IstekHatasi && hata.durum === 401) localStorage.removeItem(TOKEN_ANAHTARI)
    }).finally(() => {
      if (etkin) hazirAyarla(true)
    })
    return () => { etkin = false }
  }, [])

  async function girisYap(eposta: string, sifre: string) {
    const yanit = await api.giris(eposta, sifre)
    localStorage.setItem(TOKEN_ANAHTARI, yanit.token)
    kullaniciAyarla(yanit.kullanici)
  }

  async function kayitOl(ad: string, eposta: string, sifre: string) {
    const yanit = await api.kayit(ad, eposta, sifre)
    localStorage.setItem(TOKEN_ANAHTARI, yanit.token)
    kullaniciAyarla(yanit.kullanici)
  }

  function cikisYap() {
    localStorage.removeItem(TOKEN_ANAHTARI)
    kullaniciAyarla(null)
  }

  return <OturumBaglami.Provider value={{ kullanici, hazir, girisYap, kayitOl, cikisYap }}>{children}</OturumBaglami.Provider>
}

export function useOturum() {
  const baglam = useContext(OturumBaglami)
  if (!baglam) throw new Error('Oturum bağlamı bulunamadı.')
  return baglam
}
